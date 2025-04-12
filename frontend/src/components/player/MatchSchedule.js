import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { 
  Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, 
  Typography, Box, CircularProgress, Chip, Alert, Tab, Tabs
} from '@mui/material';
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
    fetchPlayerMatches();
  }, []);

  const fetchPlayerMatches = async () => {
    setLoading(true);
    try {
      const res = await axios.get(API_ENDPOINTS.MATCHES.GET_BY_PLAYER(auth.user.id));
      setMatches(res.data);
      setError(null);
    } catch (err) {
      setError('Error fetching matches. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleRowClick = (matchId) => {
    navigate(`/player/matches/${matchId}`);
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
    switch(tabValue) {
      case 0: // All matches
        return true;
      case 1: // Upcoming matches
        return match.status === 'SCHEDULED';
      case 2: // In progress matches
        return match.status === 'IN_PROGRESS';
      case 3: // Completed matches
        return match.status === 'COMPLETED';
      default:
        return true;
    }
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
          View your upcoming, in-progress, and past matches.
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
          <Typography variant="body1" color="textSecondary" sx={{ mt: 2 }}>
            {tabValue === 1 
              ? "You don't have any upcoming matches scheduled."
              : tabValue === 2 
                ? "You don't have any matches in progress right now."
                : tabValue === 3
                  ? "You haven't completed any matches yet."
                  : "You don't have any matches."}
          </Typography>
        </Paper>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Tournament</TableCell>
                <TableCell>Opponent</TableCell>
                <TableCell>Date & Time</TableCell>
                <TableCell>Court</TableCell>
                <TableCell>Referee</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredMatches.map((match) => {
                const isPlayer1 = match.player1Id === auth.user.id;
                const opponent = isPlayer1 ? match.player2Name : match.player1Name;
                
                return (
                  <TableRow 
                    key={match.id}
                    hover
                    onClick={() => handleRowClick(match.id)}
                    sx={{ cursor: 'pointer' }}
                  >
                    <TableCell>{match.tournamentName}</TableCell>
                    <TableCell>{opponent}</TableCell>
                    <TableCell>{formatDateTime(match.scheduledTime)}</TableCell>
                    <TableCell>{match.courtNumber}</TableCell>
                    <TableCell>{match.refereeName}</TableCell>
                    <TableCell>
                      <Chip 
                        label={match.status.replace('_', ' ')} 
                        color={getStatusColor(match.status)}
                        size="small"
                      />
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  );
};

export default MatchSchedule;