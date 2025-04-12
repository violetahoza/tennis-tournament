import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
import {
  Paper, Button, Typography, Box, Divider, Alert, CircularProgress,
  Grid, TextField, Card, CardContent, FormControl, InputLabel, Select, MenuItem,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip
} from '@mui/material';
import { ArrowBack as ArrowBackIcon, Save as SaveIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';

const MatchDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const isNewMatch = id === 'new' || id === undefined;
  const preselectedTournamentId = location.state?.tournamentId;

  const [match, setMatch] = useState({
    tournamentId: preselectedTournamentId || '',
    player1Id: '',
    player2Id: '',
    refereeId: '',
    courtNumber: '',
    scheduledTime: new Date().toISOString().slice(0, 16), // Format: YYYY-MM-DDThh:mm
    status: 'SCHEDULED',
    round: 'ROUND_1'
  });
  
  const [tournaments, setTournaments] = useState([]);
  const [players, setPlayers] = useState([]);
  const [referees, setReferees] = useState([]);
  const [scores, setScores] = useState([]);
  
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    Promise.all([
      fetchTournaments(),
      fetchPlayers(),
      fetchReferees()
    ]).then(() => {
      if (!isNewMatch) {
        return Promise.all([
          fetchMatch(),
          fetchScores()
        ]);
      }
    }).then(() => {
      setLoading(false);
    }).catch(error => {
      console.error('Error fetching data:', error);
      setError('An error occurred while fetching data.');
      setLoading(false);
    });
  }, [id, isNewMatch]);

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
      setPlayers(res.data);
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

  const handleChange = (e) => {
    const { name, value } = e.target;
    setMatch({ ...match, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validation
    if (match.player1Id === match.player2Id) {
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

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
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
        <Typography variant="h5">
          {isNewMatch ? 'Add New Match' : 'Edit Match'}
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      <form onSubmit={handleSubmit}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
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
                {tournaments.map((tournament) => (
                  <MenuItem key={tournament.id} value={tournament.id}>
                    {tournament.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
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
                {players.map((player) => (
                  <MenuItem key={player.id} value={player.id}>
                    {player.firstName} {player.lastName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
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
                {players.map((player) => (
                  <MenuItem key={player.id} value={player.id}>
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