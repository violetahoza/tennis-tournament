import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
import {
  Paper, Button, Typography, Box, Divider, Alert, CircularProgress,
  Grid, TextField, Card, CardContent, FormControl, InputLabel, Select, MenuItem,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip
} from '@mui/material';
import { ArrowBack as ArrowBackIcon, Save as SaveIcon, EmojiEvents as WinnerIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';
import TournamentRegistrationEditor from './TournamentRegistrationEditor';

const MatchDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const isNewMatch = id === 'new' || id === undefined;
  const preselectedTournamentId = location.state?.tournamentId || '';

  const [match, setMatch] = useState({
    tournamentId: preselectedTournamentId,
    player1Id: '',
    player2Id: '',
    refereeId: '',
    courtNumber: '',
    scheduledTime: new Date().toISOString().slice(0, 16), // Format: YYYY-MM-DDThh:mm
    status: 'SCHEDULED',
    round: 'ROUND_1'
  });
  
  const [tournaments, setTournaments] = useState([]);
  const [allPlayers, setAllPlayers] = useState([]);
  const [eligiblePlayers, setEligiblePlayers] = useState([]);
  const [referees, setReferees] = useState([]);
  const [scores, setScores] = useState([]);
  const [registeredPlayers, setRegisteredPlayers] = useState([]);
  
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [matchWinner, setMatchWinner] = useState(null);
  const [loadedInitialData, setLoadedInitialData] = useState(false);

  useEffect(() => {
    // Load base data first
    Promise.all([
      fetchTournaments(),
      fetchPlayers(),
      fetchReferees()
    ]).then(() => {
      setLoadedInitialData(true);
      if (!isNewMatch) {
        return Promise.all([
          fetchMatch(),
          fetchScores()
        ]);
      }
      return Promise.resolve();
    }).then(() => {
      setLoading(false);
    }).catch(error => {
      console.error('Error fetching data:', error);
      setError('An error occurred while fetching data.');
      setLoading(false);
    });
  }, [id, isNewMatch]);

  // Effect to fetch registered players when tournament changes or after initial data is loaded
  useEffect(() => {
    if (match.tournamentId && loadedInitialData) {
      console.log("Fetching registered players for tournament:", match.tournamentId);
      fetchRegisteredPlayers(match.tournamentId);
    }
  }, [match.tournamentId, loadedInitialData]);

  // Calculate winner when scores change or match is loaded
  useEffect(() => {
    if (!isNewMatch && match.status === 'COMPLETED' && scores.length > 0) {
      calculateMatchWinner();
    }
  }, [scores, match]);

  const fetchTournaments = async () => {
    try {
      const res = await axios.get(API_ENDPOINTS.TOURNAMENTS.GET_ALL);
      setTournaments(res.data);
      return res.data;
    } catch (err) {
      console.error('Error fetching tournaments:', err);
      throw err;
    }
  };

  const fetchPlayers = async () => {
    try {
      const res = await axios.get(API_ENDPOINTS.USERS.GET_BY_TYPE('PLAYER'));
      setAllPlayers(res.data);
      return res.data;
    } catch (err) {
      console.error('Error fetching players:', err);
      throw err;
    }
  };

  const fetchReferees = async () => {
    try {
      const res = await axios.get(API_ENDPOINTS.USERS.GET_BY_TYPE('REFEREE'));
      setReferees(res.data);
      return res.data;
    } catch (err) {
      console.error('Error fetching referees:', err);
      throw err;
    }
  };

  const fetchMatch = async () => {
    try {
      const res = await axios.get(API_ENDPOINTS.MATCHES.GET_BY_ID(id));
      
      // Format date to local datetime format for input
      const matchData = {
        ...res.data,
        scheduledTime: new Date(res.data.scheduledTime).toISOString().slice(0, 16)
      };
      
      setMatch(matchData);
      return matchData;
    } catch (err) {
      console.error('Error fetching match:', err);
      throw err;
    }
  };

  const fetchScores = async () => {
    try {
      const res = await axios.get(API_ENDPOINTS.MATCH_SCORES.GET_BY_MATCH(id));
      setScores(res.data);
      return res.data;
    } catch (err) {
      console.error('Error fetching scores:', err);
      return [];
    }
  };

  const fetchRegisteredPlayers = async (tournamentId) => {
    console.log("Fetching registered players...");
    try {
      const res = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_TOURNAMENT(tournamentId));
      console.log("Registration data received:", res.data);
      
      // Filter for approved registrations only
      const approvedRegistrations = res.data.filter(reg => reg.status === 'APPROVED');
      console.log("Approved registrations:", approvedRegistrations);
      
      const approvedPlayerIds = approvedRegistrations.map(reg => reg.playerId);
      console.log("Approved player IDs:", approvedPlayerIds);
      
      // Filter the all players list to get only eligible players
      const eligiblePlayersList = allPlayers.filter(player => 
        approvedPlayerIds.includes(player.id)
      );
      console.log("Eligible players:", eligiblePlayersList);
      
      setRegisteredPlayers(approvedRegistrations);
      setEligiblePlayers(eligiblePlayersList);
      
      // If the current selected players are not in the eligible list, reset them
      if (!isNewMatch) {
        // For existing matches, keep the players as they are
        return;
      }
      
      const updatedMatch = { ...match };
      let matchUpdated = false;
      
      if (updatedMatch.player1Id && !approvedPlayerIds.includes(updatedMatch.player1Id)) {
        updatedMatch.player1Id = '';
        matchUpdated = true;
      }
      
      if (updatedMatch.player2Id && !approvedPlayerIds.includes(updatedMatch.player2Id)) {
        updatedMatch.player2Id = '';
        matchUpdated = true;
      }
      
      if (matchUpdated) {
        setMatch(updatedMatch);
      }
      
    } catch (err) {
      console.error('Error fetching registered players:', err);
      setEligiblePlayers([]);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setMatch({ ...match, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validation
    if (match.player1Id === match.player2Id && match.player1Id !== '') {
      setError('A player cannot play against themselves');
      return;
    }
    
    setSaving(true);
    setError(null);
    setSuccess(null);
    
    try {
      let response;
      
      if (isNewMatch) {
        response = await axios.post(API_ENDPOINTS.MATCHES.CREATE, match);
        setSuccess('Match created successfully!');
        
        // Redirect to match list
        setTimeout(() => {
          navigate('/admin/matches');
        }, 1500);
      } else {
        response = await axios.put(API_ENDPOINTS.MATCHES.UPDATE(id), match);
        setMatch({
          ...response.data,
          scheduledTime: new Date(response.data.scheduledTime).toISOString().slice(0, 16)
        });
        setSuccess('Match updated successfully!');
      }
      
    } catch (err) {
      setError(err.response?.data?.message || 'Error saving match. Please try again.');
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  const handleBack = () => {
    navigate('/admin/matches');
  };

  const handleRegistrationStatusUpdated = () => {
    // Refresh the eligible players list
    fetchRegisteredPlayers(match.tournamentId);
  };

  const calculateMatchWinner = () => {
    if (!scores || scores.length === 0) {
      setMatchWinner(null);
      return;
    }
    
    let player1Sets = 0;
    let player2Sets = 0;
    
    scores.forEach(score => {
      if (score.player1Score > score.player2Score) {
        player1Sets++;
      } else if (score.player2Score > score.player1Score) {
        player2Sets++;
      }
    });
    
    if (player1Sets > player2Sets) {
      setMatchWinner({
        name: match.player1Name,
        score: player1Sets,
        loserScore: player2Sets
      });
    } else if (player2Sets > player1Sets) {
      setMatchWinner({
        name: match.player2Name,
        score: player2Sets,
        loserScore: player1Sets
      });
    } else {
      setMatchWinner(null);
    }
  };

  const getStatusColor = (status) => {
    switch(status) {
      case 'SCHEDULED': return 'info';
      case 'IN_PROGRESS': return 'warning';
      case 'COMPLETED': return 'success';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };

  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return 'N/A';
    const date = new Date(dateTimeString);
    return date.toLocaleString();
  };

  // Get player names for display in scores
  const getPlayerName = (playerId) => {
    const player = allPlayers.find(p => p.id === playerId);
    return player ? `${player.firstName} ${player.lastName}` : 'Unknown Player';
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  // Determine which players to show in dropdowns
  const playersToShow = (isNewMatch && match.tournamentId) ? eligiblePlayers : allPlayers;

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
        <Typography variant="h5">
          {isNewMatch ? 'Add New Match' : 'Edit Match'}
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}
      
      {/* Display winner when match is completed */}
      {!isNewMatch && match.status === 'COMPLETED' && matchWinner && (
        <Card sx={{ mb: 3, backgroundColor: '#f8f9fa' }}>
          <CardContent sx={{ textAlign: 'center' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 1 }}>
              <WinnerIcon sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="h6" color="success.main">
                Match Result
              </Typography>
            </Box>
            <Typography variant="h5" gutterBottom>
              Winner: {matchWinner.name}
            </Typography>
            <Typography variant="body1">
              Final Score: {matchWinner.score} - {matchWinner.loserScore}
            </Typography>
          </CardContent>
        </Card>
      )}

      <form onSubmit={handleSubmit}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <FormControl fullWidth required>
                <InputLabel id="tournament-label">Tournament</InputLabel>
                <Select
                  labelId="tournament-label"
                  name="tournamentId"
                  value={match.tournamentId}
                  label="Tournament"
                  onChange={handleChange}
                  disabled={!isNewMatch} // Can't change tournament after creation
                >
                  <MenuItem value="">Select Tournament</MenuItem>
                  {tournaments.map((tournament) => (
                    <MenuItem key={tournament.id} value={tournament.id}>
                      {tournament.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              
              {match.tournamentId && (
                <TournamentRegistrationEditor 
                  tournamentId={match.tournamentId}
                  onStatusUpdated={handleRegistrationStatusUpdated}
                />
              )}
            </Box>
          </Grid>
          
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required>
              <InputLabel id="player1-label">Player 1</InputLabel>
              <Select
                labelId="player1-label"
                name="player1Id"
                value={match.player1Id}
                label="Player 1"
                onChange={handleChange}
                disabled={!isNewMatch && match.status !== 'SCHEDULED'} // Can only change players for scheduled matches
              >
                <MenuItem value="">Select Player 1</MenuItem>
                {playersToShow.map((player) => (
                  <MenuItem key={player.id} value={player.id}>
                    {player.firstName} {player.lastName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {isNewMatch && match.tournamentId && eligiblePlayers.length === 0 && (
              <Typography variant="caption" color="error">
                No approved players available. Approve player registrations first.
              </Typography>
            )}
          </Grid>
          
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required>
              <InputLabel id="player2-label">Player 2</InputLabel>
              <Select
                labelId="player2-label"
                name="player2Id"
                value={match.player2Id}
                label="Player 2"
                onChange={handleChange}
                disabled={!isNewMatch && match.status !== 'SCHEDULED'} // Can only change players for scheduled matches
              >
                <MenuItem value="">Select Player 2</MenuItem>
                {playersToShow.map((player) => (
                  <MenuItem 
                    key={player.id} 
                    value={player.id}
                    disabled={player.id === match.player1Id} // Prevent selecting same player
                  >
                    {player.firstName} {player.lastName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12}>
            <FormControl fullWidth required>
              <InputLabel id="referee-label">Referee</InputLabel>
              <Select
                labelId="referee-label"
                name="refereeId"
                value={match.refereeId}
                label="Referee"
                onChange={handleChange}
                disabled={!isNewMatch && match.status !== 'SCHEDULED'} // Can only change referee for scheduled matches
              >
                <MenuItem value="">Select Referee</MenuItem>
                {referees.map((referee) => (
                  <MenuItem key={referee.id} value={referee.id}>
                    {referee.firstName} {referee.lastName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12} sm={6}>
            <TextField
              name="courtNumber"
              label="Court Number"
              type="number"
              fullWidth
              value={match.courtNumber}
              onChange={handleChange}
              inputProps={{ min: 1 }}
              required
            />
          </Grid>
          
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required>
              <InputLabel id="round-label">Round</InputLabel>
              <Select
                labelId="round-label"
                name="round"
                value={match.round}
                label="Round"
                onChange={handleChange}
              >
                <MenuItem value="ROUND_1">Round 1</MenuItem>
                <MenuItem value="ROUND_2">Round 2</MenuItem>
                <MenuItem value="QUARTER_FINAL">Quarter Final</MenuItem>
                <MenuItem value="SEMI_FINAL">Semi Final</MenuItem>
                <MenuItem value="FINAL">Final</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12}>
            <TextField
              name="scheduledTime"
              label="Match Date and Time"
              type="datetime-local"
              fullWidth
              value={match.scheduledTime}
              onChange={handleChange}
              InputLabelProps={{
                shrink: true,
              }}
              required
            />
          </Grid>
          
          {!isNewMatch && (
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel id="status-label">Status</InputLabel>
                <Select
                  labelId="status-label"
                  name="status"
                  value={match.status}
                  label="Status"
                  onChange={handleChange}
                >
                  <MenuItem value="SCHEDULED">Scheduled</MenuItem>
                  <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                  <MenuItem value="COMPLETED">Completed</MenuItem>
                  <MenuItem value="CANCELLED">Cancelled</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          )}
          
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                color="primary"
                type="submit"
                startIcon={<SaveIcon />}
                disabled={saving}
              >
                {saving ? 'Saving...' : 'Save Match'}
              </Button>
            </Box>
          </Grid>
        </Grid>
      </form>

      {!isNewMatch && scores.length > 0 && (
        <>
          <Box sx={{ mt: 4, mb: 2 }}>
            <Typography variant="h6">Match Scores</Typography>
          </Box>
          
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Set</TableCell>
                      <TableCell align="center">{match.player1Name}</TableCell>
                      <TableCell align="center">{match.player2Name}</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {scores.map((score) => (
                      <TableRow key={score.id}>
                        <TableCell>{score.setNumber}</TableCell>
                        <TableCell align="center">{score.player1Score}</TableCell>
                        <TableCell align="center">{score.player2Score}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </>
      )}
    </Paper>
  );
};

export default MatchDetails;