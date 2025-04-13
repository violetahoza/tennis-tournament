// Define API endpoints for the application
export const API_BASE_URL = 'http://localhost:8080';

export const API_ENDPOINTS = {
  // Auth endpoints
  AUTH: {
    LOGIN: `${API_BASE_URL}/api/auth/signin`,
    REGISTER: `${API_BASE_URL}/api/auth/signup`,
    ME: `${API_BASE_URL}/api/auth/me`,
  },
  
  // User endpoints
  USERS: {
    GET_ALL: `${API_BASE_URL}/api/users`,
    GET_BY_ID: (id) => `${API_BASE_URL}/api/users/${id}`,
    GET_BY_TYPE: (type) => `${API_BASE_URL}/api/users/type/${type}`,
    UPDATE: (id) => `${API_BASE_URL}/api/users/${id}`,
    UPDATE_PASSWORD: (id) => `${API_BASE_URL}/api/users/${id}/password`,
    DELETE: (id) => `${API_BASE_URL}/api/users/${id}`,
  },
  
  // Tournament endpoints
  TOURNAMENTS: {
    GET_ALL: `${API_BASE_URL}/api/tournaments`,
    GET_UPCOMING: `${API_BASE_URL}/api/tournaments/upcoming`,
    GET_REGISTRATION_OPEN: `${API_BASE_URL}/api/tournaments/registration-open`,
    GET_BY_ID: (id) => `${API_BASE_URL}/api/tournaments/${id}`,
    CREATE: `${API_BASE_URL}/api/tournaments`,
    UPDATE: (id) => `${API_BASE_URL}/api/tournaments/${id}`,
    DELETE: (id) => `${API_BASE_URL}/api/tournaments/${id}`,
    GET_PARTICIPANTS_COUNT: (id) => `${API_BASE_URL}/api/tournaments/${id}/participants-count`,
  },
  
  // Tournament Registration endpoints
  TOURNAMENT_REGISTRATIONS: {
    GET_BY_PLAYER: (playerId) => `${API_BASE_URL}/api/tournament-registrations/player/${playerId}`,
    GET_BY_TOURNAMENT: (tournamentId) => `${API_BASE_URL}/api/tournament-registrations/tournament/${tournamentId}`,
    REGISTER: (playerId, tournamentId) => `${API_BASE_URL}/api/tournament-registrations/player/${playerId}/tournament/${tournamentId}`,
    UPDATE_STATUS: (registrationId) => `${API_BASE_URL}/api/tournament-registrations/${registrationId}/status`,
    CANCEL: (registrationId) => `${API_BASE_URL}/api/tournament-registrations/${registrationId}`,
  },
  
  // Match endpoints
  MATCHES: {
    GET_ALL: `${API_BASE_URL}/api/matches`,
    GET_BY_TOURNAMENT: (tournamentId) => `${API_BASE_URL}/api/matches/tournament/${tournamentId}`,
    GET_BY_PLAYER: (playerId) => `${API_BASE_URL}/api/matches/player/${playerId}`,
    GET_BY_REFEREE: (refereeId) => `${API_BASE_URL}/api/matches/referee/${refereeId}`,
    GET_BY_ID: (id) => `${API_BASE_URL}/api/matches/${id}`,
    GET_SUMMARY: (id) => `${API_BASE_URL}/api/matches/${id}/summary`,
    CREATE: `${API_BASE_URL}/api/matches`,
    UPDATE: (id) => `${API_BASE_URL}/api/matches/${id}`,
    DELETE: (id) => `${API_BASE_URL}/api/matches/${id}`,
  },
  
  // Match Score endpoints
  MATCH_SCORES: {
    GET_BY_MATCH: (matchId) => `${API_BASE_URL}/api/match-scores/match/${matchId}`,
    GET_BY_ID: (id) => `${API_BASE_URL}/api/match-scores/${id}`,
    CREATE: `${API_BASE_URL}/api/match-scores`,
    UPDATE: (id) => `${API_BASE_URL}/api/match-scores/${id}`,
    DELETE: (id) => `${API_BASE_URL}/api/match-scores/${id}`,
    COMPLETE_MATCH: (matchId) => `${API_BASE_URL}/api/match-scores/match/${matchId}/complete`,
  },
  
  // Report endpoints
  REPORTS: {
    EXPORT_MATCHES_CSV: `${API_BASE_URL}/api/reports/matches/csv`,
    EXPORT_MATCHES_TXT: `${API_BASE_URL}/api/reports/matches/txt`,
  },

  NOTIFICATIONS: {
    GET_ALL: `${API_BASE_URL}/api/notifications`,
    MARK_AS_READ: (id) => `${API_BASE_URL}/api/notifications/${id}/read`,
    MARK_ALL_AS_READ: `${API_BASE_URL}/api/notifications/read-all`,
  },
  
  WEBSOCKET: `${API_BASE_URL}/ws`

};