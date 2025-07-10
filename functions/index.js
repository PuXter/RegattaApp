const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.scheduledRoomCleanup = functions.pubsub
  .schedule("every 1 hours")
  .onRun(async () => {
    const now = admin.firestore.Timestamp.now();
    const twelveHoursAgo = admin.firestore.Timestamp.fromMillis(
      now.toMillis() - 12 * 60 * 60 * 1000
    );

    const roomsRef = admin.firestore().collection("rooms");
    const snapshot = await roomsRef
      .where("createdAt", "<", twelveHoursAgo)
      .get();

    const deletions = [];
    snapshot.forEach((doc) => {
      console.log(`Deleting room: ${doc.id}`);
      deletions.push(doc.ref.delete());
    });

    await Promise.all(deletions);
    console.log(`Deleted ${deletions.length} rooms older than 12 hours.`);
    return null;
  });