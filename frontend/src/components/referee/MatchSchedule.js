import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { 
  Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, 
  Button, Typography, Box, CircularProgress, Chip, Alert, Tab, Tabs
} from '@mui/material';
import { ScoreboardOutlined } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';
import { AuthContext } from '../../context/AuthContext';

const MatchSchedule = () => {
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tabValue, setTabValue] = useState(0);
  
  const navigate = useNavigate();
  const { auth } = useContext(AuthContext);

  useEffect(() => {
    fetchRefereeMatches();
  }, []);

  const fetchRefereeMatches = async () => {
    setLoading(true);
    try {
      // Get all matches assigned to this referee
      const res = await axios.get(API_ENDPOINTS.MATCHES.GET_BY_REFEREE(auth.user.id));
      setMatches(res.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching matches:', err);
      setError('Error fetching matches. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleScoreMatch = (matchId) => {
    navigate(`/referee/matches/${matchId}/score`);
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

  const getStatusColor = (status) => {
    switch(status) {
      case 'SCHEDULED': return 'info';
      case 'IN_PROGRESS': return 'warning';
      case 'COMPLETED': return 'success';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };

  // Filter matches based on tab
  const filteredMatches = matches.filter(match => {
    if (tabValue === 0) { // All matches
      return true;
    } else if (tabValue === 1) { // Upcoming matches
      return match.status === 'SCHEDULED';
    } else if (tabValue === 2) { // In progress
      return match.status === 'IN_PROGRESS';
    } else if (tabValue === 3) { // Completed
      return match.status === 'COMPLETED';
    }
    return true;
  });

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          My Match Schedule
        </Typography>
        <Typography variant="body1" color="textSecondary">
          View and manage matches you're assigned to referee.
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Paper sx={{ mb: 3 }}>
        <Tabs value={tabValue} onChange={handleTabChange} indicatorColor="primary" textColor="primary">
          <Tab label="All Matches" />
          <Tab label="Upcoming" />
          <Tab label="In Progress" />
          <Tab label="Completed" />
        </Tabs>
      </Paper>

      {filteredMatches.length === 0 ? (
        <Paper sx={{ p: 3, textAlign: 'center' }}>
          <Typography variant="h6">No matches found</Typography>
        </Paper>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Tournament</TableCell>
                <TableCell>Players</TableCell>
                <TableCell>Date & Time</TableCell>
                <TableCell>Court</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredMatches.map((match) => (
                <TableRow key={match.id}>
                  <TableCell>{match.tournamentName}</TableCell>
                  <TableCell>{match.player1Name} vs {match.player2Name}</TableCell>
                  <TableCell>{formatDateTime(match.scheduledTime)}</TableCell>
                  <TableCell>{match.courtNumber}</TableCell>
                  <TableCell>
                    <Chip 
                      label={match.status} 
                      color={getStatusColor(match.status)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Button
                      variant="outlined"
                      color="primary"
                      size="small"
                      startIcon={<ScoreboardOutlined />}
                      onClick={() => handleScoreMatch(match.id)}
                      disabled={match.status === 'CANCELLED'}
                    >
                      {match.status === 'COMPLETED' ? 'View Score' : 'Manage Score'}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  );
};

export default MatchSchedule;