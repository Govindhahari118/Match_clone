const USER_ROOM_PREFIX = 'user:';
const MATCH_ROOM_PREFIX = 'match:';

function buildUserRoom(userId) {
    return `${USER_ROOM_PREFIX}${userId}`;
}

function buildMatchRoom(matchId) {
    return `${MATCH_ROOM_PREFIX}${matchId}`;
}

module.exports = {
    USER_ROOM_PREFIX,
    MATCH_ROOM_PREFIX,
    buildUserRoom,
    buildMatchRoom,
};
