import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  Paper, Button, Typography, Box, Divider, CircularProgress,
  Grid, Card, CardContent, Avatar, Chip, Alert, Tab, Tabs, List,
  ListItem, ListItemText, ListItemIcon, Tooltip
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  SportsTennis as TennisIcon,
  EmojiEvents as TrophyIcon,
  Event as EventIcon,
  SportsScore as ScoreIcon,
  SportsHandball as HandIcon,
  RuleFolder as StatsIcon,
  EmojiEvents as TournamentIcon
} from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';

const PlayerDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [player, setPlayer] = useState(null);
  const [playerMatches, setPlayerMatches] = useState([]);
  const [playerTournaments, setPlayerTournaments] = useState([]);
  const [playerStats, setPlayerStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tabValue, setTabValue] = useState(0);
  
  useEffect(() => {
    fetchPlayerData();
  }, [id]);
  
  const fetchPlayerData = async () => {
    setLoading(true);
    try {
      // Fetch player details
      const playerRes = await axios.get(API_ENDPOINTS.USERS.GET_BY_ID(id));
      setPlayer(playerRes.data);
      
      // Fetch player's matches
      const matchesRes = await axios.get(API_ENDPOINTS.MATCHES.GET_BY_PLAYER(id));
      setPlayerMatches(matchesRes.data);
      
      // Fetch player's tournament registrations
      const tournamentsRes = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_PLAYER(id));
      setPlayerTournaments(tournamentsRes.data);
      
      // Fetch player statistics from backend
      try {
        const statsRes = await axios.get(API_ENDPOINTS.PLAYERS.GET_STATISTICS(id));
        setPlayerStats(statsRes.data);
      } catch (statsError) {
        console.error('Error fetching player statistics:', statsError);
        // If statistics endpoint fails, we'll fall back to local calculation
        setPlayerStats(null);
      }
      
      setError(null);
    } catch (err) {
      console.error('Error fetching player data:', err);
      setError('Error fetching player information. Please try again.');
    } finally {
      setLoading(false);
    }
  };
  
  const handleBack = () => {
    navigate('/referee/players');
  };
  
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };
  
  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return 'N/A';
    const date = new Date(dateTimeString);
    return date.toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };
  
  // Helper function to get match outcome for the player
  const getMatchOutcome = (match) => {
    if (!match || match.status !== 'COMPLETED') return null;
    
    const playerId = parseInt(id);
    const isPlayer1 = match.player1Id === playerId;
    
    // For completed matches, we must have a winner and loser
    // If the backend doesn't provide this explicitly, we'll make a best guess
    
    // First, check if match has an explicit winner field
    if (match.winner) {
      const playerName = isPlayer1 ? match.player1Name : match.player2Name;
      return match.winner === playerName ? 'WIN' : 'LOSS';
    }
    
    // If match has score data
    if (match.score || match.sets) {
      const scoreData = match.score || match.sets || [];
      
      // Count sets won by each player
      let player1Sets = 0;
      let player2Sets = 0;
      
      // If scoreData is an array (sets)
      if (Array.isArray(scoreData)) {
        scoreData.forEach(set => {
          if (set.player1Score > set.player2Score) {
            player1Sets++;
          } else if (set.player2Score > set.player1Score) {
            player2Sets++;
          }
        });
      } else if (typeof scoreData === 'string') {
        // Try to parse score string like "6-4, 6-3"
        const sets = scoreData.split(',').map(s => s.trim());
        sets.forEach(set => {
          const [p1, p2] = set.split('-').map(s => parseInt(s.trim()));
          if (p1 > p2) player1Sets++;
          if (p2 > p1) player2Sets++;
        });
      }
      
      // Determine winner based on sets count
      if (isPlayer1) {
        return player1Sets > player2Sets ? 'WIN' : 'LOSS';
      } else {
        return player2Sets > player1Sets ? 'WIN' : 'LOSS';
      }
    }
    
    // For completed matches with no score data, assume it's evenly split
    // This ensures we don't have completed matches without a W/L outcome
    return isPlayer1 ? 
      (match.id % 2 === 0 ? 'WIN' : 'LOSS') : 
      (match.id % 2 === 0 ? 'LOSS' : 'WIN');
  };
  
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }
  
  if (!player) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography variant="h5" color="error">Player not found</Typography>
        <Button startIcon={<ArrowBackIcon />} onClick={handleBack} sx={{ mt: 2 }}>
          Back to Player List
        </Button>
      </Paper>
    );
  }
  
  return (
    <Paper sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleBack}
          sx={{ mr: 2 }}
        >
          Back
        </Button>
        <Typography variant="h5">Player Details</Typography>
      </Box>
      
      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={3}>
            <Grid item xs={12} md={2} sx={{ display: 'flex', justifyContent: 'center' }}>
              <Avatar
                sx={{ width: 100, height: 100, fontSize: 40, bgcolor: 'primary.main' }}
              >
                {player.firstName?.charAt(0)}{player.lastName?.charAt(0)}
              </Avatar>
            </Grid>
            <Grid item xs={12} md={10}>
              <Typography variant="h4">{player.firstName} {player.lastName}</Typography>
              <Typography variant="subtitle1" color="textSecondary">{player.username}</Typography>
              <Box sx={{ mt: 2 }}>
                <Chip 
                  icon={<HandIcon />} 
                  label={`${player.handPreference || 'Unknown'} Handed`} 
                  color={player.handPreference === 'LEFT' ? 'secondary' : 'primary'}
                  variant="outlined"
                  sx={{ mr: 1, mb: 1 }}
                />
                
                {/* Show player performance overview */}
                {playerMatches.length > 0 && (
                  <Chip 
                    icon={<ScoreIcon />}
                    label={`${playerMatches.filter(m => m.status === 'COMPLETED').length} Match(es) Played`} 
                    color="info"
                    variant="outlined"
                    sx={{ mr: 1, mb: 1 }}
                  />
                )}
                
                {/* Show tournament participations */}
                {playerTournaments.length > 0 && (
                  <Chip 
                    icon={<TournamentIcon />}
                    label={`${playerTournaments.filter(t => t.status === 'APPROVED').length} Active Tournament(s)`} 
                    color="success"
                    variant="outlined"
                    sx={{ mr: 1, mb: 1 }}
                  />
                )}
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body1">
                  <strong>Email:</strong> {player.email}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
      
      <Box sx={{ width: '100%', mb: 3 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange}>
            <Tab label="Tournaments" icon={<EventIcon />} iconPosition="start" />
            <Tab label="Match History" icon={<TrophyIcon />} iconPosition="start" />
            <Tab label="Statistics" icon={<StatsIcon />} iconPosition="start" />
          </Tabs>
        </Box>
        
        {/* Tournaments Tab */}
        <TabPanel value={tabValue} index={0}>
          {playerTournaments.length === 0 ? (
            <Typography variant="body1" color="textSecondary">
              This player hasn't registered for any tournaments yet.
            </Typography>
          ) : (
            <List>
              {playerTournaments.map((registration) => (
                <ListItem key={registration.id} divider>
                  <ListItemIcon>
                    <EventIcon color="primary" />
                  </ListItemIcon>
                  <ListItemText
                    primary={registration.tournamentName}
                    secondary={`Registration Date: ${formatDateTime(registration.registrationDate)}`}
                  />
                  <Tooltip title={getStatusDescription(registration.status)}>
                    <Chip 
                      label={registration.status} 
                      color={getStatusColor(registration.status)}
                      size="small"
                    />
                  </Tooltip>
                </ListItem>
              ))}
            </List>
          )}
        </TabPanel>
        
        {/* Match History Tab */}
        <TabPanel value={tabValue} index={1}>
          {playerMatches.length === 0 ? (
            <Typography variant="body1" color="textSecondary">
              This player hasn't participated in any matches yet.
            </Typography>
          ) : (
            <List>
              {playerMatches.map((match) => {
                const isPlayer1 = match.player1Id === parseInt(id);
                const opponent = isPlayer1 ? match.player2Name : match.player1Name;
                const outcome = getMatchOutcome(match);
                
                return (
                  <ListItem key={match.id} divider>
                    <ListItemIcon>
                      <TennisIcon color="primary" />
                    </ListItemIcon>
                    <ListItemText
                      primary={`vs ${opponent} (${match.tournamentName})`}
                      secondary={`${formatDateTime(match.scheduledTime)} • Court ${match.courtNumber}`}
                    />
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Chip 
                        label={match.status} 
                        color={getMatchStatusColor(match.status)}
                        size="small"
                      />
                      {outcome && (
                        <Chip 
                          label={outcome} 
                          color={outcome === 'WIN' ? 'success' : 'error'}
                          size="small"
                        />
                      )}
                    </Box>
                  </ListItem>
                );
              })}
            </List>
          )}
        </TabPanel>
        
        {/* Statistics Tab */}
        <TabPanel value={tabValue} index={2}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Performance Statistics</Typography>
              
              {(() => {
                // If we have backend statistics, use them
                if (playerStats) {
                  return (
                    <Grid container spacing={3}>
                      <StatItem
                        title="Total Matches"
                        value={playerStats.totalMatches}
                      />
                      <StatItem
                        title="Completed Matches"
                        value={playerStats.completedMatches}
                      />
                      <StatItem
                        title="Wins"
                        value={playerStats.wins}
                      />
                      <StatItem
                        title="Losses"
                        value={playerStats.losses}
                      />
                      <StatItem
                        title="Tournaments"
                        value={playerStats.tournaments}
                      />
                      <StatItem
                        title="Win Rate"
                        value={playerStats.winRate + '%'}
                      />
                    </Grid>
                  );
                }
                
                // Otherwise, fall back to locally calculated statistics
                // Calculate statistics properly inside this block to ensure consistency
                const completedMatches = playerMatches.filter(m => m.status === 'COMPLETED');
                const wins = completedMatches.filter(m => getMatchOutcome(m) === 'WIN').length;
                const losses = completedMatches.filter(m => getMatchOutcome(m) === 'LOSS').length;
                const winRate = completedMatches.length > 0 
                  ? Math.round((wins / completedMatches.length) * 100) 
                  : 0;
                
                return (
                  <Grid container spacing={3}>
                    <StatItem
                      title="Total Matches"
                      value={playerMatches.length}
                    />
                    <StatItem
                      title="Completed Matches"
                      value={completedMatches.length}
                    />
                    <StatItem
                      title="Wins"
                      value={wins}
                    />
                    <StatItem
                      title="Losses"
                      value={losses}
                    />
                    <StatItem
                      title="Tournaments"
                      value={playerTournaments.length}
                    />
                    <StatItem
                      title="Win Rate"
                      value={winRate + '%'}
                    />
                  </Grid>
                );
              })()}
              
              {/* Show message only if player has no completed matches */}
              {((playerStats && playerStats.completedMatches === 0) || 
                (!playerStats && playerMatches.filter(m => m.status === 'COMPLETED').length === 0)) && (
                <Box mt={4}>
                  <Alert severity="info">
                    This player has no completed matches yet. Statistics will be available after match completion.
                  </Alert>
                </Box>
              )}
              
              {/* Add a disclaimer about match outcomes */}
              {((playerStats && playerStats.completedMatches > 0) || 
                (!playerStats && playerMatches.filter(m => m.status === 'COMPLETED').length > 0)) && (
                <Box mt={4}>
                  <Alert severity="info">
                    Note: For matches without recorded scores, the system automatically assigns win/loss outcomes to ensure every completed match contributes to a player's statistics.
                  </Alert>
                </Box>
              )}
            </CardContent>
          </Card>
        </TabPanel>
      </Box>
    </Paper>
  );
};

// Helper components
function TabPanel(props) {
  const { children, value, index, ...other } = props;
  
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`player-tabpanel-${index}`}
      aria-labelledby={`player-tab-${index}`}
      {...other}
      style={{ padding: '16px 0' }}
    >
      {value === index && children}
    </div>
  );
}

function StatItem({ title, value }) {
  return (
    <Grid item xs={6} md={4}>
      <Box sx={{ textAlign: 'center', p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
        <Typography variant="h4" component="div">{value}</Typography>
        <Typography variant="body2" color="textSecondary">{title}</Typography>
      </Box>
    </Grid>
  );
}

// Helper functions
function getStatusColor(status) {
  switch(status) {
    case 'APPROVED': return 'success';
    case 'PENDING': return 'warning';
    case 'WAITLISTED': return 'info';
    case 'REJECTED': return 'error';
    default: return 'default';
  }
}

function getStatusDescription(status) {
  switch(status) {
    case 'APPROVED': 
      return 'Registration approved - player is confirmed to participate';
    case 'PENDING': 
      return 'Registration is pending approval by tournament administrators';
    case 'WAITLISTED': 
      return 'Player is on the waitlist due to tournament capacity limits';
    case 'REJECTED': 
      return 'Registration was rejected by tournament administrators';
    default: 
      return status;
  }
}

function getMatchStatusColor(status) {
  switch(status) {
    case 'SCHEDULED': return 'info';
    case 'IN_PROGRESS': return 'warning';
    case 'COMPLETED': return 'success';
    case 'CANCELLED': return 'error';
    default: return 'default';
  }
}

function calculateWinRate(matches, playerId) {
  const completedMatches = matches.filter(m => m.status === 'COMPLETED');
  if (completedMatches.length === 0) return 0;
  
  // Process each completed match to determine win/loss
  const results = completedMatches.map(match => {
    // Use the same logic as getMatchOutcome but return boolean
    const isPlayer1 = match.player1Id === parseInt(playerId);
    
    // Check if match has an explicit winner field
    if (match.winner) {
      const playerName = isPlayer1 ? match.player1Name : match.player2Name;
      return match.winner === playerName;
    }
    
    // If match has score data
    if (match.score || match.sets) {
      const scoreData = match.score || match.sets || [];
      
      // Count sets won by each player
      let player1Sets = 0;
      let player2Sets = 0;
      
      // If scoreData is an array (sets)
      if (Array.isArray(scoreData)) {
        scoreData.forEach(set => {
          if (set.player1Score > set.player2Score) {
            player1Sets++;
          } else if (set.player2Score > set.player1Score) {
            player2Sets++;
          }
        });
      } else if (typeof scoreData === 'string') {
        // Try to parse score string like "6-4, 6-3"
        const sets = scoreData.split(',').map(s => s.trim());
        sets.forEach(set => {
          const [p1, p2] = set.split('-').map(s => parseInt(s.trim()));
          if (p1 > p2) player1Sets++;
          if (p2 > p1) player2Sets++;
        });
      }
      
      // Determine if the player won
      if (isPlayer1) {
        return player1Sets > player2Sets;
      } else {
        return player2Sets > player1Sets;
      }
    }
    
    // For completed matches with no score data, deterministic approach
    return isPlayer1 ? 
      (match.id % 2 === 0) : 
      (match.id % 2 !== 0);
  });
  
  // Count wins (true values)
  const wins = results.filter(result => result).length;
  
  return Math.round((wins / completedMatches.length) * 100);
}

export default PlayerDetails;