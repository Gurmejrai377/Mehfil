const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

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

test("Unauthenticated users are denied across all Mehfil collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("profiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("rooms").where("publicRoom", "==", true).get());
  await assertFails(unauthDb.collection("chats").where("participantUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("calls").where("participantUids", "array-contains", ALICE_UID).get());
});

test("UserAccount & PublicProfile isolate PII and enforce ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      mehfilId: "mehfil_alice1",
      bio: "Yahan har baat ek Mehfil hai",
      online: true,
      lowDataMode: false,
      coinBalance: 5000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user PII read rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Public profile creation + discoverable query succeeds
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      mehfilId: "mehfil_alice1",
      statusText: "On Mehfil",
      avatarColorHex: "#6E2CF2",
      online: true,
      discoverable: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("profiles").where("discoverable", "==", true).get());
  await assertFails(bobDb.collection("profiles").get()); // Unfiltered query fails
});

test("Chats & ChatMessages support text, voice audioDataBase64, and participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const chatId = "dm_alice_123_bob_456";
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participantUids: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      chatKind: "DIRECT",
      title: "Alice & Bob",
      lastMessageText: "Salam!",
      lastMessageType: "TEXT",
      lastMessageSenderId: ALICE_UID,
      unreadCount: 0,
      pinned: false,
      muted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      senderId: BOB_UID,
      senderName: "Bob",
      participantUids: [ALICE_UID, BOB_UID],
      messageType: "VOICE",
      text: "Voice note",
      mediaDurationSec: 4,
      audioDataBase64: "AAAAFGZ0eXBNNEEgAAAA",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Scoped queries match rules
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages")
      .where("participantUids", "array-contains", ALICE_UID).get()
  );

  // Non-participant cannot read, list unfiltered, list Alice's filter, or inject messages
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").get());
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages")
      .where("participantUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_hijack").set({
      chatId,
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      participantUids: [CHARLIE_UID],
      messageType: "TEXT",
      text: "Hijack",
      mediaDurationSec: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Calls support WebRTC offer/answer/ICE signaling and lock terminal states", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const callId = "call_1";
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("calls").doc(callId).set({
      callerId: ALICE_UID,
      callerName: "Alice",
      calleeId: BOB_UID,
      calleeName: "Bob",
      participantUids: [ALICE_UID, BOB_UID],
      callType: "VIDEO",
      status: "RINGING",
      qualityMode: "HD",
      durationSeconds: 0,
      offerSdp: "v=0\r\no=- 123 2 IN IP4 127.0.0.1\r\n",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("calls").doc(callId).collection("candidates").doc("cand_1").set({
      callId,
      senderId: ALICE_UID,
      participantUids: [ALICE_UID, BOB_UID],
      sdpMid: "0",
      sdpMLineIndex: 0,
      sdpCandidate: "candidate:1 1 UDP 2122252543 192.168.1.1 50000 typ host",
      createdAt: ts,
    })
  );

  // Bob answers
  await assertSucceeds(
    bobDb.collection("calls").doc(callId).update({
      status: "ACTIVE",
      answerSdp: "v=0\r\no=- 456 2 IN IP4 127.0.0.1\r\n",
      updatedAt: pastDate(2000),
    })
  );

  // Alice ends
  await assertSucceeds(
    aliceDb.collection("calls").doc(callId).update({
      status: "ENDED",
      durationSeconds: 18,
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state locking
  await assertFails(
    aliceDb.collection("calls").doc(callId).update({
      status: "ACTIVE",
      updatedAt: pastDate(500),
    })
  );
});

test("PartyRooms, 8 Mic Seats, Chat, Gifts, Song Queue, and Audio Signals enforce rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const roomId = "room_party_1";
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).set({
      name: "Royal Gold Mehfil",
      roomType: "PARTY",
      hostId: ALICE_UID,
      hostName: "Alice",
      topic: "Live party",
      publicRoom: true,
      activeMemberUids: [ALICE_UID],
      viewerCount: 1,
      currentSongTitle: "Kesariya Lo-Fi",
      musicPlaying: true,
      hostMicOn: true,
      totalGiftCoins: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Host creates seat 0
  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).collection("seats").doc("seat_0").set({
      roomId,
      seatIndex: 0,
      publicRoom: true,
      avatarColorHex: "#6E2CF2",
      muted: false,
      giftCoins: 0,
      updatedAt: ts,
    })
  );

  // Bob claims empty seat 0
  await assertSucceeds(
    bobDb.collection("rooms").doc(roomId).collection("seats").doc("seat_0").update({
      occupantId: BOB_UID,
      occupantName: "Bob",
      avatarColorHex: "#6E2CF2",
      muted: false,
      updatedAt: pastDate(2000),
    })
  );

  // Charlie cannot hijack Bob's seat
  await assertFails(
    charlieDb.collection("rooms").doc(roomId).collection("seats").doc("seat_0").update({
      occupantId: CHARLIE_UID,
      occupantName: "Charlie",
      updatedAt: pastDate(1000),
    })
  );

  // Bob mutes own seat & sends message
  await assertSucceeds(
    bobDb.collection("rooms").doc(roomId).collection("seats").doc("seat_0").update({
      muted: true,
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    bobDb.collection("rooms").doc(roomId).collection("messages").doc("rmsg_1").set({
      roomId,
      senderId: BOB_UID,
      senderName: "Bob",
      kind: "TEXT",
      text: "Hello Mehfil!",
      publicRoom: true,
      createdAt: ts,
    })
  );

  // WebRTC room signal scoped to pair
  await assertSucceeds(
    bobDb.collection("rooms").doc(roomId).collection("signals").doc("sig_1").set({
      roomId,
      fromUid: BOB_UID,
      toUid: ALICE_UID,
      participantUids: [BOB_UID, ALICE_UID],
      signalType: "OFFER",
      payload: "v=0...",
      createdAt: ts,
    })
  );

  await assertFails(
    charlieDb.collection("rooms").doc(roomId).collection("signals")
      .where("participantUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).collection("signals")
      .where("participantUids", "array-contains", ALICE_UID).get()
  );

  // Public room filtered query succeeds, unfiltered fails
  await assertSucceeds(bobDb.collection("rooms").where("publicRoom", "==", true).get());
  await assertFails(bobDb.collection("rooms").get());
});
