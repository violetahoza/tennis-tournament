import React, { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  Paper, Button, Typography, Box, Divider, Alert, CircularProgress,
  Grid, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, Card, CardContent
} from '@mui/material';
import { ArrowBack as ArrowBackIcon, EmojiEvents as WinnerIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';
import { AuthContext } from '../../context/AuthContext';

const MatchDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { auth } = useContext(AuthContext);
  
  const [match, setMatch] = useState(null);
  const [scores, setScores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [matchWinner, setMatchWinner] = useState(null);

  useEffect(() => {
    Promise.all([
      fetchMatch(),
      fetchScores()
    ]).then(() => {
      setLoading(false);
    }).catch(error => {
      console.error('Error fetching data:', error);
      setError('An error occurred while fetching data.');
      setLoading(false);
    });
  }, [id]);

  // Calculate winner whenever scores or match changes
  useEffect(() => {
    if (match && match.status === 'COMPLETED' && scores.length > 0) {
      const winnerInfo = calculateMatchWinner();
      setMatchWinner(winnerInfo);
    }
  }, [match, scores]);

  const fetchMatch = async () => {
    try {
      const res = await axios.get(API_ENDPOINTS.MATCHES.GET_BY_ID(id));
      setMatch(res.data);
      return res.data;
    } catch (err) {
      console.error('Error fetching match:', err);
      setError('Error fetching match details. Please try again.');
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

  const handleBack = () => {
    navigate('/player/matches');
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

  // Determine if the current user is player1 or player2
  const isPlayer1 = match && auth.user && match.player1Id === auth.user.id;
  const isPlayer2 = match && auth.user && match.player2Id === auth.user.id;

  // Calculate match winner
  const calculateMatchWinner = () => {
    if (!scores || scores.length === 0 || !match) {
      return null;
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
      return {
        name: match.player1Name,
        score: player1Sets,
        loserName: match.player2Name,
        loserScore: player2Sets,
        isCurrentPlayerWinner: isPlayer1
      };
    } else if (player2Sets > player1Sets) {
      return {
        name: match.player2Name,
        score: player2Sets,
        loserName: match.player1Name,
        loserScore: player1Sets,
        isCurrentPlayerWinner: isPlayer2
      };
    } else {
      return null; // Tie (shouldn't happen in completed matches)
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
      <Alert severity="error">
        Match not found. <Button onClick={handleBack}>Go Back</Button>
      </Alert>
    );
  }

  // Determine the opponent's name based on which player the user is
  const opponentName = isPlayer1 ? match.player2Name : match.player1Name;

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
            Match Details
          </Typography>
        </Box>
        <Chip 
          label={match.status} 
          color={getStatusColor(match.status)}
        />
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Tournament: {match.tournamentName}</Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Typography variant="body1" gutterBottom><strong>Date & Time:</strong> {formatDateTime(match.scheduledTime)}</Typography>
              <Typography variant="body1" gutterBottom><strong>Court:</strong> {match.courtNumber}</Typography>
              <Typography variant="body1" gutterBottom><strong>Round:</strong> {match.round}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="body1" gutterBottom><strong>Opponent:</strong> {opponentName}</Typography>
              <Typography variant="body1" gutterBottom><strong>Referee:</strong> {match.refereeName}</Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Winner display for completed matches */}
      {match.status === 'COMPLETED' && matchWinner && (
        <Card sx={{ mb: 3, backgroundColor: matchWinner.isCurrentPlayerWinner ? '#e8f5e9' : '#fff8e1' }}>
          <CardContent sx={{ textAlign: 'center' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 1 }}>
              <WinnerIcon sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="h6" color="success.main">
                Match Result
              </Typography>
            </Box>
            <Typography variant="h5" gutterBottom>
              Winner: {matchWinner.name} {matchWinner.isCurrentPlayerWinner && '(You)'}
            </Typography>
            <Typography variant="body1">
              Final Score: {matchWinner.score} - {matchWinner.loserScore}
            </Typography>
          </CardContent>
        </Card>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Match Score</Typography>
          
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
                  </TableRow>
                </TableHead>
                <TableBody>
                  {scores.map((score) => (
                    <TableRow key={score.id}>
                      <TableCell>{score.setNumber}</TableCell>
                      <TableCell align="center">
                        {score.player1Score}
                        {isPlayer1 && score.player1Score > score.player2Score && ' ✓'}
                      </TableCell>
                      <TableCell align="center">
                        {score.player2Score}
                        {isPlayer2 && score.player2Score > score.player1Score && ' ✓'}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Paper>
  );
};

export default MatchDetails;