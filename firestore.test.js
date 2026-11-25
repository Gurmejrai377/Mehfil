const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function recentDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated users: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("profiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("chats").where("participantUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("calls").where("participantUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("stories").where("viewerUids", "array-contains", ALICE_UID).get());
});

test("Users & Profiles: PII isolated to owner, public profile discoverable via filtered query", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = recentDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      waveId: "wave_alice",
      bio: "Available on WaveCall",
      online: true,
      lowDataMode: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      bio: "Hijacked",
      updatedAt: recentDate(500),
    })
  );

  // Reject ghost field (Shadow Update test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: recentDate(500),
    })
  );

  // Public profile without PII
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      waveId: "wave_alice",
      statusText: "HD video ready",
      avatarColorHex: "#0088FF",
      online: true,
      discoverable: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered query succeeds; unfiltered query fails
  await assertSucceeds(bobDb.collection("profiles").where("discoverable", "==", true).get());
  await assertFails(bobDb.collection("profiles").get());
});

test("Personal Contacts: strict owner isolation under /users/{userId}/contacts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = recentDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").doc("contact_bob").set({
      userId: ALICE_UID,
      peerId: BOB_UID,
      displayName: "Bob",
      waveId: "wave_bob",
      statusText: "Hey there",
      avatarColorHex: "#00B894",
      online: true,
      favorite: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("contacts").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("contacts").get());
});

test("Chats & Messages: participant isolation, denormalized member check, and query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = recentDate(5000);
  const chatId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participantUids: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      chatKind: "DIRECT",
      title: "Alice & Bob",
      lastMessageText: "Hey Bob!",
      lastMessageType: "TEXT",
      lastMessageSenderId: ALICE_UID,
      unreadCount: 1,
      pinned: false,
      muted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Participant queries succeed; unfiltered & outsider queries fail
  await assertSucceeds(
    bobDb.collection("chats").where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertFails(bobDb.collection("chats").get());
  await assertFails(charlieDb.collection("chats").doc(chatId).get());

  // Send message mirroring Kotlin payload (no `id`, no explicit null optional keys)
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participantUids: [ALICE_UID, BOB_UID],
      messageType: "TEXT",
      text: "Video call in 5?",
      mediaDurationSec: 0,
      createdAt: recentDate(2000),
      updatedAt: recentDate(2000),
    })
  );

  // Denormalized query succeeds for Bob, fails for Charlie
  await assertSucceeds(
    bobDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .where("participantUids", "array-contains", BOB_UID)
      .get()
  );
  // Charlie cannot read Alice/Bob's message directly, run unfiltered list, or query with Alice's UID
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").get()
  );
  await assertFails(
    charlieDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .where("participantUids", "array-contains", ALICE_UID)
      .get()
  );

  // Reaction update allowed for participant; immutable senderId rejected
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").update({
      reaction: "❤️",
      updatedAt: recentDate(500),
    })
  );
  await assertFails(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").update({
      senderId: BOB_UID,
      updatedAt: recentDate(500),
    })
  );
});

test("Calls: signaling lifecycle & terminal state locking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = recentDate(5000);

  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      callerId: ALICE_UID,
      callerName: "Alice",
      calleeId: BOB_UID,
      calleeName: "Bob",
      participantUids: [ALICE_UID, BOB_UID],
      callType: "VIDEO",
      status: "RINGING",
      qualityMode: "HD",
      durationSeconds: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob answers -> ACTIVE, then Alice ends -> ENDED
  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").update({
      status: "ACTIVE",
      updatedAt: recentDate(2000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").update({
      status: "ENDED",
      durationSeconds: 42,
      updatedAt: recentDate(500),
    })
  );

  // Terminal state lock: cannot update ENDED call
  await assertFails(
    aliceDb.collection("calls").doc("call_1").update({
      durationSeconds: 999,
      updatedAt: recentDate(100),
    })
  );

  // Outsider denied
  await assertFails(charlieDb.collection("calls").doc("call_1").get());
  await assertSucceeds(
    bobDb.collection("calls").where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertFails(bobDb.collection("calls").get());
});

test("Stories: viewer-scoped access and view counter updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = recentDate(3000);

  await assertSucceeds(
    aliceDb.collection("stories").doc("story_1").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      caption: "Coffee & HD calls",
      moodEmoji: "☕",
      accentHex: "#0079FE",
      viewerUids: [ALICE_UID, BOB_UID],
      viewCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("stories").where("viewerUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("stories").doc("story_1").update({
      viewCount: 1,
      updatedAt: recentDate(500),
    })
  );
  await assertFails(charlieDb.collection("stories").doc("story_1").get());
  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("stories").doc("story_future").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      caption: "Future",
      moodEmoji: "🚀",
      accentHex: "#0079FE",
      viewerUids: [ALICE_UID],
      viewCount: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
