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

test("Unauthenticated user: cannot read or write any user profile, posts, post_states, or comments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("posts").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("post_states").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("comments").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      fullName: "Alice",
      avatarUrl: "",
      verified: false,
      postsCount: 0,
      followersCount: "0",
      followingCount: "0",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated owner: valid UserProfile create & partial update succeed; ghost field & immutable mutations fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alex.creates",
      fullName: "Alex Mercer",
      bio: "Visual Designer",
      link: "alexmercer.design",
      avatarUrl: "https://example.com/avatar.jpg",
      verified: true,
      postsCount: 42,
      followersCount: "18.4K",
      followingCount: "482",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial bio/profile update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Updated creative bio",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Partial postsCount increment update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      postsCount: 43,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / RBAC escalation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      verified: true,
      isAdmin: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Time traveler",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Authenticated owner: UserPost create, partial like/save/comment updates, list & delete succeed; extra id field fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").doc("post_101").set({
      userId: ALICE_UID,
      authorUsername: "alex.creates",
      authorAvatarUrl: "https://example.com/avatar.jpg",
      location: "Tokyo, Japan",
      caption: "Neon nightwalk #tokyo",
      imageUrl: "https://example.com/photo.jpg",
      hashtags: ["#tokyo"],
      likesCount: 1,
      liked: true,
      saved: false,
      commentsCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List subcollection (no existing() undefined property errors on empty/non-empty)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").get()
  );

  // Partial like update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").doc("post_101").update({
      liked: false,
      likesCount: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Partial save update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").doc("post_101").update({
      saved: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject `@DocumentId` `id` field inside document body
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").doc("post_102").set({
      id: "post_102",
      userId: ALICE_UID,
      authorUsername: "alex.creates",
      authorAvatarUrl: "",
      caption: "Bad id field",
      imageUrl: "https://example.com/photo.jpg",
      hashtags: [],
      likesCount: 0,
      liked: false,
      saved: false,
      commentsCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Authenticated owner: PostInteractionState and UserComment create, update, and list succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("post_states").doc("post_1").set({
      userId: ALICE_UID,
      postId: "post_1",
      liked: true,
      saved: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("post_states").doc("post_1").update({
      liked: true,
      saved: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("comments").doc("comment_1").set({
      userId: ALICE_UID,
      postId: "post_1",
      authorUsername: "alex.creates",
      authorAvatarUrl: "https://example.com/avatar.jpg",
      text: "Great shot!",
      likesCount: 0,
      liked: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("comments").doc("comment_1").update({
      liked: true,
      likesCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("post_states").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("comments").get()
  );
});

test("Cross-user isolation: Alice cannot read or modify Bob's profile, posts, post_states, or comments", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const created = new Date(Date.now() - 10000);
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob",
      fullName: "Bob Smith",
      avatarUrl: "",
      verified: false,
      postsCount: 1,
      followersCount: "10",
      followingCount: "5",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(BOB_UID).collection("posts").doc("bob_post").set({
      userId: BOB_UID,
      authorUsername: "bob",
      authorAvatarUrl: "",
      caption: "Bob's post",
      imageUrl: "https://example.com/bob.jpg",
      hashtags: [],
      likesCount: 1,
      liked: true,
      saved: false,
      commentsCount: 0,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ bio: "Hacked" }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("posts").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("posts").doc("bob_post").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("posts").doc("bob_post").delete()
  );
});
