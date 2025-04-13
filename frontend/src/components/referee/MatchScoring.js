import React, { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  Paper, Button, Typography, Box, Divider, CircularProgress,
  Grid, TextField, Card, CardContent, TableContainer, Table, TableBody,
  TableCell, TableHead, TableRow, Chip, Dialog, DialogActions, 
  DialogContent, DialogContentText, DialogTitle, Snackbar, Alert
} from '@mui/material';
import { ArrowBack as ArrowBackIcon, Add as AddIcon, Delete as DeleteIcon, 
         Save as SaveIcon, EmojiEvents as WinnerIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';
import { AuthContext } from '../../context/AuthContext';

const MatchScoring = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { auth, logout } = useContext(AuthContext);
  
  const [match, setMatch] = useState(null);
  const [scores, setScores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  
  // Form states
  const [newSet, setNewSet] = useState({
    matchId: id,
    setNumber: 1,
    player1Score: 0,
    player2Score: 0
  });
  
  // Dialog states
  const [openSetDialog, setOpenSetDialog] = useState(false);
  const [openCompleteDialog, setOpenCompleteDialog] = useState(false);
  const [openErrorDialog, setOpenErrorDialog] = useState(false);
  const [openSuccessDialog, setOpenSuccessDialog] = useState(false);
  const [dialogMessage, setDialogMessage] = useState('');
  
  // Score summary state for the completion dialog
  const [scoreSummary, setScoreSummary] = useState({ player1Sets: 0, player2Sets: 0, winner: null });
  
  // Snackbar states
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [snackbarSeverity, setSnackbarSeverity] = useState('info');

  useEffect(() => {
    fetchMatchData();
  }, [id]);

  useEffect(() => {
    // Set next set number based on existing scores
    if (scores.length > 0) {
      const maxSetNumber = Math.max(...scores.map(s => s.setNumber));
      setNewSet(prev => ({ ...prev, setNumber: maxSetNumber + 1 }));
    } else {
      setNewSet(prev => ({ ...prev, setNumber: 1 }));
    }
  }, [scores]);

  useEffect(() => {
    // Set up authorization header for all axios requests
    if (auth && auth.token) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${auth.token}`;
    } else {
      delete axios.defaults.headers.common['Authorization'];
    }
  }, [auth]);

  const fetchMatchData = async () => {
    setLoading(true);
    try {
      const [matchData, scoresData] = await Promise.all([
        fetchMatch(),
        fetchScores()
      ]);
    } catch (error) {
      console.error('Error fetching data:', error);
      showErrorDialog('An error occurred while fetching match data.');
    } finally {
      setLoading(false);
    }
  };

  const fetchMatch = async () => {
    try {
      console.log(`Fetching match data for match ID: ${id}`);
      const res = await axios.get(API_ENDPOINTS.MATCHES.GET_BY_ID(id));
      console.log('Match data response:', res.data);
      setMatch(res.data);
      return res.data;
    } catch (err) {
      console.error('Error fetching match:', err);
      showErrorDialog('Error fetching match details. Please try again.');
      throw err;
    }
  };

  const fetchScores = async () => {
    try {
      console.log(`Fetching scores for match ID: ${id}`);
      const res = await axios.get(API_ENDPOINTS.MATCH_SCORES.GET_BY_MATCH(id));
      console.log('Scores data response:', res.data);
      setScores(res.data);
      return res.data;
    } catch (err) {
      console.error('Error fetching scores:', err);
      return [];
    }
  };

  const handleBack = () => {
    navigate('/referee/matches');
  };

  const showErrorDialog = (message) => {
    setDialogMessage(message);
    setOpenErrorDialog(true);
  };

  const showSuccessDialog = (message) => {
    setDialogMessage(message);
    setOpenSuccessDialog(true);
  };

  const showSnackbar = (message, severity = 'info') => {
    setSnackbarMessage(message);
    setSnackbarSeverity(severity);
    setSnackbarOpen(true);
  };

  const handleAddSet = async () => {
    try {
      validateSetScore();
      setSubmitting(true);
      
      // Log the request payload for debugging
      console.log('Creating new set with data:', newSet);
      
      try {
        const res = await axios.post(API_ENDPOINTS.MATCH_SCORES.CREATE, newSet);
        console.log('Score creation response:', res);
        
        // Only proceed if we have a valid response
        if (res && res.data) {
          // Add the new score to the local state
          setScores([...scores, res.data]);
          showSuccessDialog('Set score added successfully!');
          setOpenSetDialog(false);
          
          // If match is SCHEDULED, update to IN_PROGRESS
          if (match && match.status === 'SCHEDULED') {
            await updateMatchStatus('IN_PROGRESS');
          }
          
          // Refresh the scores to ensure consistency
          fetchScores();
        } else {
          // This should rarely happen, but handle it just in case
          throw new Error('Invalid response from server');
        }
      } catch (err) {
        console.error('Error response:', err.response);
        
        // Check if we actually got an error response from the server
        if (err.response) {
          const statusCode = err.response.status;
          
          if (statusCode === 403) {
            showErrorDialog('Access denied. You do not have permission to add scores or your session may have expired. Please try logging out and back in.');
          } else if (statusCode === 401) {
            showErrorDialog('Your session has expired. Please log out and log back in to continue scoring the match.');
            
            setTimeout(() => {
              logout();
              navigate('/login');
            }, 3000);
          } else {
            // For other error codes, show the server's error message if available
            showErrorDialog(err.response.data?.message || 'Error adding set score. Please try again.');
          }
        } else if (err.request) {
          // The request was made but no response was received
          showErrorDialog('No response from server. Please check your connection and try again.');
        } else {
          // Something happened in setting up the request that triggered an Error
          showErrorDialog('Error preparing request. Please try again.');
        }
      }
    } catch (validationError) {
      // This handles errors from the validateSetScore function
      showErrorDialog(validationError.message || 'Invalid score values. Please check your input.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteSet = async (scoreId) => {
    try {
      setSubmitting(true);
      
      console.log(`Deleting score with ID: ${scoreId}`);
      try {
        await axios.delete(API_ENDPOINTS.MATCH_SCORES.DELETE(scoreId));
        setScores(scores.filter(score => score.id !== scoreId));
        showSnackbar('Set score deleted successfully!', 'success');
      } catch (err) {
        console.error('Error deleting score:', err.response || err);
        
        if (err.response && (err.response.status === 401 || err.response.status === 403)) {
          showErrorDialog('Permission denied when deleting score. Your session may have expired.');
        } else {
          showErrorDialog(err.response?.data?.message || 'Error deleting set score. Please try again.');
        }
      }
    } catch (err) {
      showErrorDialog('An unexpected error occurred. Please try again.');
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const calculateWinner = () => {
    if (!match) return null;
    
    let player1Sets = 0;
    let player2Sets = 0;
    
    scores.forEach(score => {
      if (score.player1Score > score.player2Score) {
        player1Sets++;
      } else if (score.player2Score > score.player1Score) {
        player2Sets++;
      }
    });
    
    let winnerName = null;
    if (player1Sets > player2Sets) {
      winnerName = match.player1Name;
    } else if (player2Sets > player1Sets) {
      winnerName = match.player2Name;
    }
    
    return {
      player1Sets,
      player2Sets,
      winnerName,
      isTie: player1Sets === player2Sets
    };
  };

  const handleOpenCompleteDialog = () => {
    // Calculate the current score summary
    const summary = calculateWinner();
    setScoreSummary(summary);
    
    // Only open the dialog if there's a clear winner
    if (summary.isTie) {
      showErrorDialog('Cannot complete the match with tied scores. There must be a winner.');
    } else {
      setOpenCompleteDialog(true);
    }
  };

  const handleCompleteMatch = async () => {
    try {
      // Double-check that there are scores recorded and there's a winner
      if (scores.length === 0) {
        showErrorDialog('Cannot complete a match without any scores recorded.');
        setOpenCompleteDialog(false);
        return;
      }
      
      // Determine if there's a clear winner
      const winner = calculateWinner();
      
      if (winner.isTie) {
        showErrorDialog('Cannot complete the match with tied scores. There must be a winner.');
        setOpenCompleteDialog(false);
        return;
      }
      
      setSubmitting(true);
      
      try {
        console.log('Completing match:', id);
        const res = await axios.post(API_ENDPOINTS.MATCH_SCORES.COMPLETE_MATCH(id));
        console.log('Match completion response:', res);
        
        // Update match object
        setMatch(prev => ({
          ...prev,
          status: 'COMPLETED'
        }));
        
        showSuccessDialog(`Match completed successfully! Winner: ${winner.winnerName}`);
        setOpenCompleteDialog(false);
      } catch (err) {
        console.error('Complete match error:', err.response || err);
        
        if (err.response) {
          const statusCode = err.response.status;
          
          if (statusCode === 403) {
            showErrorDialog('Access denied. You do not have permission to complete this match or your session may have expired.');
          } else if (statusCode === 401) {
            showErrorDialog('Your session has expired. Please log out and log back in.');
          } else {
            showErrorDialog(err.response.data?.message || 'Error completing match. Please try again.');
          }
        } else {
          showErrorDialog('Network error or server unavailable. Please check your connection and try again.');
        }
      }
    } catch (err) {
      showErrorDialog(err.message || 'Error completing match. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const updateMatchStatus = async (status) => {
    try {
      // We need to update the whole match object
      const updatedMatch = {
        ...match,
        status: status
      };
      
      console.log('Updating match status to:', status);
      console.log('Request payload:', updatedMatch);
      
      const res = await axios.put(API_ENDPOINTS.MATCHES.UPDATE(id), updatedMatch);
      console.log('Update status response:', res);
      
      setMatch(prev => ({ ...prev, status }));
      return res.data;
    } catch (err) {
      console.error('Error updating match status:', err);
      
      // Check for authorization errors
      if (err.response && (err.response.status === 401 || err.response.status === 403)) {
        showErrorDialog('Permission denied when updating match status. Your session may have expired.');
      } else {
        showErrorDialog('Failed to update match status. Please try again.');
      }
      
      throw err;
    }
  };

  const validateSetScore = () => {
    if (newSet.player1Score < 0 || newSet.player2Score < 0) {
      throw new Error('Games cannot be negative');
    }
    
    // Basic tennis scoring validation
    if (newSet.player1Score === newSet.player2Score) {
      throw new Error('Sets cannot end in a tie in tennis');
    }
    
    if (newSet.player1Score > 7 || newSet.player2Score > 7) {
      throw new Error('Maximum game score in a set is 7');
    }
    
    // Case 1: One player has 6 games
    if ((newSet.player1Score === 6 && newSet.player2Score < 5) || 
        (newSet.player2Score === 6 && newSet.player1Score < 5)) {
      // This is a valid score (6-0, 6-1, 6-2, 6-3, 6-4)
      return;
    }
    
    // Case 2: 7-5 score
    if ((newSet.player1Score === 7 && newSet.player2Score === 5) || 
        (newSet.player2Score === 7 && newSet.player1Score === 5)) {
      // This is a valid score (7-5)
      return;
    }
    
    // Case 3: 7-6 score (tiebreak)
    if ((newSet.player1Score === 7 && newSet.player2Score === 6) || 
        (newSet.player2Score === 7 && newSet.player1Score === 6)) {
      // This is a valid score (7-6)
      return;
    }
    
    // If we reach here, the score is invalid
    throw new Error('Invalid tennis set score. Valid scores include 6-0 through 6-4, 7-5, and 7-6.');
  };

  const handleOpenSetDialog = () => {
    setOpenSetDialog(true);
  };

  const handleCloseSetDialog = () => {
    setOpenSetDialog(false);
  };
  
  const handleCloseCompleteDialog = () => {
    setOpenCompleteDialog(false);
  };
  
  const handleCloseErrorDialog = () => {
    setOpenErrorDialog(false);
  };
  
  const handleCloseSuccessDialog = () => {
    setOpenSuccessDialog(false);
  };
  
  const handleSnackbarClose = () => {
    setSnackbarOpen(false);
  };
  
  const handleSetChange = (e) => {
    const { name, value } = e.target;
    setNewSet({
      ...newSet,
      [name]: parseInt(value, 10) || 0
    });
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

  const getStatusColor = (status) => {
    switch(status) {
      case 'SCHEDULED': return 'info';
      case 'IN_PROGRESS': return 'warning';
      case 'COMPLETED': return 'success';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };
  
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!match) {
    return (
      <Dialog open={true} onClose={handleBack}>
        <DialogTitle>Match Not Found</DialogTitle>
        <DialogContent>
          <DialogContentText>
            The requested match could not be found.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleBack}>Go Back</Button>
        </DialogActions>
      </Dialog>
    );
  }

  const winner = calculateWinner();
  const player1Sets = scores.reduce((acc, score) => acc + (score.player1Score > score.player2Score ? 1 : 0), 0);
  const player2Sets = scores.reduce((acc, score) => acc + (score.player2Score > score.player1Score ? 1 : 0), 0);

  return (
    <Paper sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={handleBack}
            sx={{ mr: 2 }}
          >
            Back
          </Button>
          <Typography variant="h5">
            Match Scoring
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {match.status === 'COMPLETED' && winner && winner.winnerName && (
            <Chip 
              icon={<WinnerIcon />}
              label={`Winner: ${winner.winnerName}`}
              color="success"
              variant="outlined"
            />
          )}
          <Chip 
            label={match.status} 
            color={getStatusColor(match.status)}
          />
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>{match.tournamentName}</Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Typography variant="body1" gutterBottom><strong>Date & Time:</strong> {formatDateTime(match.scheduledTime)}</Typography>
              <Typography variant="body1" gutterBottom><strong>Court:</strong> {match.courtNumber}</Typography>
              <Typography variant="body1" gutterBottom><strong>Round:</strong> {match.round}</Typography>
              {match.status === 'COMPLETED' && winner && winner.winnerName && (
                <Typography variant="body1" gutterBottom>
                  <strong>Final Score:</strong> {player1Sets}-{player2Sets}
                </Typography>
              )}
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Players</Typography>
          <Grid container spacing={3}>
            <Grid item xs={6}>
              <Box sx={{ 
                textAlign: 'center', 
                p: 2, 
                backgroundColor: '#f5f5f5', 
                borderRadius: 1,
                border: match.status === 'COMPLETED' && winner?.winnerName === match.player1Name ? '2px solid #4caf50' : 'none'
              }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>{match.player1Name}</Typography>
                {match.status === 'COMPLETED' && (
                  <Typography variant="body2">
                    Sets won: {player1Sets}
                  </Typography>
                )}
              </Box>
            </Grid>
            <Grid item xs={6}>
              <Box sx={{ 
                textAlign: 'center', 
                p: 2, 
                backgroundColor: '#f5f5f5', 
                borderRadius: 1,
                border: match.status === 'COMPLETED' && winner?.winnerName === match.player2Name ? '2px solid #4caf50' : 'none'
              }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>{match.player2Name}</Typography>
                {match.status === 'COMPLETED' && (
                  <Typography variant="body2">
                    Sets won: {player2Sets}
                  </Typography>
                )}
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Match Score</Typography>
            {match.status !== 'COMPLETED' && (
              <Button 
                variant="contained" 
                color="primary" 
                startIcon={<AddIcon />}
                onClick={handleOpenSetDialog}
              >
                Add Set
              </Button>
            )}
          </Box>
          
          {scores.length === 0 ? (
            <Typography variant="body1" color="textSecondary">
              No scores recorded yet.
            </Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Set</TableCell>
                    <TableCell align="center">{match.player1Name}</TableCell>
                    <TableCell align="center">{match.player2Name}</TableCell>
                    {match.status !== 'COMPLETED' && <TableCell>Actions</TableCell>}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {scores.map((score) => (
                    <TableRow key={score.id}>
                      <TableCell>{score.setNumber}</TableCell>
                      <TableCell 
                        align="center"
                        sx={{ 
                          fontWeight: score.player1Score > score.player2Score ? 'bold' : 'normal',
                          color: score.player1Score > score.player2Score ? '#4caf50' : 'inherit'
                        }}
                      >
                        {score.player1Score}
                      </TableCell>
                      <TableCell 
                        align="center"
                        sx={{ 
                          fontWeight: score.player2Score > score.player1Score ? 'bold' : 'normal',
                          color: score.player2Score > score.player1Score ? '#4caf50' : 'inherit'
                        }}
                      >
                        {score.player2Score}
                      </TableCell>
                      {match.status !== 'COMPLETED' && (
                        <TableCell>
                          <Button
                            color="error"
                            size="small"
                            startIcon={<DeleteIcon />}
                            onClick={() => handleDeleteSet(score.id)}
                          >
                            Delete
                          </Button>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {scores.length > 0 && match.status !== 'COMPLETED' && (
        <Box sx={{ textAlign: 'center' }}>
          <Button
            variant="contained"
            color="primary"
            startIcon={<WinnerIcon />}
            onClick={handleOpenCompleteDialog}
            size="large"
          >
            Complete Match
          </Button>
        </Box>
      )}

      <Dialog open={openSetDialog} onClose={handleCloseSetDialog}>
        <DialogTitle>Add Set Score</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                name="setNumber"
                label="Set Number"
                type="number"
                fullWidth
                value={newSet.setNumber}
                InputProps={{ readOnly: true }}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                name="player1Score"
                label={`${match.player1Name} Games`}
                type="number"
                fullWidth
                value={newSet.player1Score}
                onChange={handleSetChange}
                inputProps={{ min: 0, max: 7 }}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                name="player2Score"
                label={`${match.player2Name} Games`}
                type="number"
                fullWidth
                value={newSet.player2Score}
                onChange={handleSetChange}
                inputProps={{ min: 0, max: 7 }}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseSetDialog}>Cancel</Button>
          <Button 
            onClick={handleAddSet} 
            variant="contained" 
            color="primary"
            disabled={submitting}
          >
            {submitting ? 'Saving...' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={openCompleteDialog} onClose={handleCloseCompleteDialog}>
        <DialogTitle>Complete Match</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to mark this match as completed?
            This action will finalize the match result and cannot be undone.
          </DialogContentText>
          {scores.length > 0 && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="subtitle1">Current Score Summary:</Typography>
              <Typography>
                {match.player1Name}: {player1Sets} sets
              </Typography>
              <Typography>
                {match.player2Name}: {player2Sets} sets
              </Typography>
              {!winner.isTie && (
                <Typography variant="subtitle1" sx={{ mt: 1, color: '#4caf50' }}>
                  Winner: {winner.winnerName}
                </Typography>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseCompleteDialog}>Cancel</Button>
          <Button 
            onClick={handleCompleteMatch} 
            variant="contained" 
            color="primary"
            disabled={submitting || winner.isTie}
          >
            {submitting ? 'Processing...' : 'Complete Match'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={openErrorDialog} onClose={handleCloseErrorDialog}>
        <DialogTitle>Error</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {dialogMessage}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseErrorDialog}>OK</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={openSuccessDialog} onClose={handleCloseSuccessDialog}>
        <DialogTitle>Success</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {dialogMessage}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseSuccessDialog}>OK</Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbarOpen}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={handleSnackbarClose} severity={snackbarSeverity} sx={{ width: '100%' }}>
          {snackbarMessage}
        </Alert>
      </Snackbar>
    </Paper>
  );
};

export default MatchScoring;