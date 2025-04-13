import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { 
  Paper, Card, CardContent, CardActions, Grid, Button, 
  Typography, Box, TextField, Chip, CircularProgress,
  Select, MenuItem, FormControl, InputLabel, Alert
} from '@mui/material';
import { API_ENDPOINTS } from '../../config';
import { AuthContext } from '../../context/AuthContext';

const TournamentList = () => {
  const [tournaments, setTournaments] = useState([]);
  const [registrations, setRegistrations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  
  const navigate = useNavigate();
  const { auth } = useContext(AuthContext);

  useEffect(() => {
    Promise.all([
      fetchTournaments(),
      fetchPlayerRegistrations()
    ]).then(() => {
      setLoading(false);
    }).catch(error => {
      console.error('Error fetching data:', error);
      setError('An error occurred while fetching data.');
      setLoading(false);
    });
  }, []);

  const fetchTournaments = async () => {
    try {
      // Use the registration-open endpoint first to get available tournaments
      const res = await axios.get(API_ENDPOINTS.TOURNAMENTS.GET_REGISTRATION_OPEN);
      setTournaments(res.data);
      return res.data;
    } catch (err) {
      console.error('Error fetching tournaments:', err);
      setError('Error fetching tournaments. Please try again.');
      throw err;
    }
  };

  const fetchPlayerRegistrations = async () => {
    if (!auth.user?.id) return [];

    try {
      const res = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_PLAYER(auth.user.id));
      setRegistrations(res.data);
      return res.data;
    } catch (err) {
      console.error('Error fetching registrations:', err);
      // Not throwing error here as it's not critical
      return [];
    }
  };

  const handleRegisterClick = (tournamentId) => {
    navigate(`/player/tournaments/${tournamentId}`);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleStatusFilterChange = (e) => {
    setStatusFilter(e.target.value);
  };

  // Filter tournaments based on search term and status filter
  const filteredTournaments = tournaments.filter(tournament => {
    const matchesSearch = searchTerm === '' || 
      tournament.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      tournament.location.toLowerCase().includes(searchTerm.toLowerCase());
    
    // Filter by registration status
    let matchesStatus = true;
    if (statusFilter === 'OPEN') {
      matchesStatus = tournament.registrationOpen === true;
    } else if (statusFilter === 'CLOSED') {
      matchesStatus = tournament.registrationOpen === false;
    }
    
    return matchesSearch && matchesStatus;
  });

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString();
  };

  const getRegistrationStatus = (tournamentId) => {
    const registration = registrations.find(reg => reg.tournamentId === tournamentId);
    return registration ? registration.status : null;
  };

  const getRegistrationStatusColor = (status) => {
    switch(status) {
      case 'PENDING': return 'warning';
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'error';
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

  return (
    <>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Available Tournaments
        </Typography>
        <Typography variant="body1" color="textSecondary">
          Browse and register for upcoming tennis tournaments.
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Box sx={{ display: 'flex', mb: 3, gap: 2 }}>
        <TextField
          label="Search Tournaments"
          variant="outlined"
          value={searchTerm}
          onChange={handleSearchChange}
          sx={{ flexGrow: 1 }}
        />
        
        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel id="status-filter-label">Registration Status</InputLabel>
          <Select
            labelId="status-filter-label"
            id="status-filter"
            value={statusFilter}
            label="Registration Status"
            onChange={handleStatusFilterChange}
          >
            <MenuItem value="">All Tournaments</MenuItem>
            <MenuItem value="OPEN">Registration Open</MenuItem>
            <MenuItem value="CLOSED">Registration Closed</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {filteredTournaments.length === 0 ? (
        <Paper sx={{ p: 3, textAlign: 'center' }}>
          <Typography variant="h6">No tournaments found</Typography>
        </Paper>
      ) : (
        <Grid container spacing={3}>
          {filteredTournaments.map((tournament) => {
            const registrationStatus = getRegistrationStatus(tournament.id);
            const isRegistrationOpen = tournament.registrationOpen;
            
            return (
              <Grid item xs={12} md={6} lg={4} key={tournament.id}>
                <Card>
                  <CardContent>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="h6" component="div">
                        {tournament.name}
                      </Typography>
                      <Chip 
                        label={isRegistrationOpen ? "Registration Open" : "Registration Closed"} 
                        color={isRegistrationOpen ? "success" : "error"}
                        size="small"
                      />
                    </Box>
                    
                    <Typography color="textSecondary" gutterBottom>
                      {tournament.location}
                    </Typography>
                    
                    <Box sx={{ mt: 2 }}>
                      <Typography variant="body2">
                        <strong>Start Date:</strong> {formatDate(tournament.startDate)}
                      </Typography>
                      <Typography variant="body2">
                        <strong>End Date:</strong> {formatDate(tournament.endDate)}
                      </Typography>
                      <Typography variant="body2">
                        <strong>Registered Players:</strong> {tournament.registeredPlayers}/{tournament.maxParticipants}
                      </Typography>
                    </Box>
                    
                    {registrationStatus && (
                      <Box sx={{ mt: 2 }}>
                        <Typography variant="body2">
                          <strong>Registration Status:</strong> {' '}
                          <Chip 
                            label={registrationStatus} 
                            color={getRegistrationStatusColor(registrationStatus)}
                            size="small"
                          />
                        </Typography>
                      </Box>
                    )}
                  </CardContent>
                  <CardActions>
                    <Button 
                      size="small" 
                      color="primary"
                      onClick={() => handleRegisterClick(tournament.id)}
                      disabled={!registrationStatus && !isRegistrationOpen}
                    >
                      {registrationStatus ? 'View Registration' : 'Register'}
                    </Button>
                  </CardActions>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}
    </>
  );
};

export default TournamentList;