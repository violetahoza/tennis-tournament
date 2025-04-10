import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  Paper, Button, TextField, Typography, Box, Alert, CircularProgress,
  Grid, Card, CardContent, Tab, Tabs, List, ListItem, ListItemText, Chip, Divider
} from '@mui/material';
import { ArrowBack as ArrowBackIcon, Save as SaveIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';

const TournamentDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isNewTournament = id === 'new';

  const [tournament, setTournament] = useState({
    name: '',
    description: '',
    location: '',
    startDate: '',
    endDate: '',
    registrationDeadline: '',
    maxParticipants: 32
  });
  
  const [registrations, setRegistrations] = useState([]);
  const [matches, setMatches] = useState([]);
  const [tabValue, setTabValue] = useState(0);
  
  const [loading, setLoading] = useState(!isNewTournament);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    if (!isNewTournament) {
      fetchTournamentData();
    }
  }, [id, isNewTournament]);

  const fetchTournamentData = async () => {
    setLoading(true);
    try {
      const tournamentRes = await axios.get(API_ENDPOINTS.TOURNAMENTS.GET_BY_ID(id));
      setTournament(tournamentRes.data);
      
      // Fetch registrations
      try {
        const registrationsRes = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_TOURNAMENT(id));
        setRegistrations(registrationsRes.data);
      } catch (err) {
        console.error('Error fetching registrations:', err);
      }
      
      // Fetch matches
      try {
        const matchesRes = await axios.get(API_ENDPOINTS.MATCHES.GET_BY_TOURNAMENT(id));
        setMatches(matchesRes.data);
      } catch (err) {
        console.error('Error fetching matches:', err);
      }
      
    } catch (err) {
      setError('Error fetching tournament details. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setTournament({ ...tournament, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validation
    if (new Date(tournament.startDate) > new Date(tournament.endDate)) {
      setError('Start date must be before end date');
      return;
    }

    if (new Date(tournament.registrationDeadline) > new Date(tournament.startDate)) {
      setError('Registration deadline must be before start date');
      return;
    }
    
    setSaving(true);
    setError(null);
    setSuccess(null);
    
    try {
      let response;
      
      // Create new tournament or update existing tournament
      if (isNewTournament) {
        response = await axios.post(API_ENDPOINTS.TOURNAMENTS.CREATE, tournament);
        setSuccess('Tournament created successfully!');
        
        // Redirect to the tournament list
        setTimeout(() => {
          navigate('/admin/tournaments');
        }, 1500);
      } else {
        response = await axios.put(API_ENDPOINTS.TOURNAMENTS.UPDATE(id), tournament);
        setTournament(response.data);
        setSuccess('Tournament updated successfully!');
      }
      
    } catch (err) {
      setError(err.response?.data?.message || 'Error saving tournament. Please try again.');
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  const handleBack = () => {
    navigate('/admin/tournaments');
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    // Convert to YYYY-MM-DD for input fields
    if (dateString.includes('T')) {
      return dateString.split('T')[0];
    }
    return dateString;
  };

  const getStatusColor = (status) => {
    switch(status) {
      case 'PENDING': return 'warning';
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'error';
      default: return 'default';
    }
  };

  const getMatchStatusColor = (status) => {
    switch(status) {
      case 'SCHEDULED': return 'info';
      case 'IN_PROGRESS': return 'warning';
      case 'COMPLETED': return 'success';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };

  const handleUpdateRegistrationStatus = async (registrationId, newStatus) => {
    try {
      await axios.put(
        API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.UPDATE_STATUS(registrationId),
        {},
        { params: { status: newStatus } }
      );
      
      // Refresh registrations
      const registrationsRes = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_TOURNAMENT(id));
      setRegistrations(registrationsRes.data);
      
      setSuccess(`Registration status updated to ${newStatus}`);
    } catch (error) {
      setError('Error updating registration status');
      console.error(error);
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
          {isNewTournament ? 'Add New Tournament' : `Edit Tournament: ${tournament.name}`}
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      {!isNewTournament && (
        <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="tournament details tabs">
            <Tab label="Tournament Details" />
            <Tab label="Registrations" />
            <Tab label="Matches" />
          </Tabs>
        </Box>
      )}

      {(tabValue === 0 || isNewTournament) && (
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                name="name"
                label="Tournament Name"
                fullWidth
                value={tournament.name}
                onChange={handleChange}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                name="description"
                label="Description"
                fullWidth
                multiline
                rows={4}
                value={tournament.description || ''}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                name="location"
                label="Location"
                fullWidth
                value={tournament.location}
                onChange={handleChange}
                required
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                name="startDate"
                label="Start Date"
                type="date"
                fullWidth
                value={formatDate(tournament.startDate)}
                onChange={handleChange}
                InputLabelProps={{
                  shrink: true,
                }}
                required
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                name="endDate"
                label="End Date"
                type="date"
                fullWidth
                value={formatDate(tournament.endDate)}
                onChange={handleChange}
                InputLabelProps={{
                  shrink: true,
                }}
                required
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                name="registrationDeadline"
                label="Registration Deadline"
                type="date"
                fullWidth
                value={formatDate(tournament.registrationDeadline)}
                onChange={handleChange}
                InputLabelProps={{
                  shrink: true,
                }}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                name="maxParticipants"
                label="Maximum Participants"
                type="number"
                fullWidth
                value={tournament.maxParticipants}
                onChange={handleChange}
                inputProps={{ min: 2 }}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  color="primary"
                  type="submit"
                  startIcon={<SaveIcon />}
                  disabled={saving}
                >
                  {saving ? 'Saving...' : 'Save Tournament'}
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      )}

      {tabValue === 1 && !isNewTournament && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>Player Registrations</Typography>
            
            {registrations.length === 0 ? (
              <Typography variant="body1" color="textSecondary">
                No registrations found for this tournament.
              </Typography>
            ) : (
              <List>
                {registrations.map((registration) => (
                  <React.Fragment key={registration.id}>
                    <ListItem>
                      <ListItemText
                        primary={registration.playerName}
                        secondary={`Registration Date: ${new Date(registration.registrationDate).toLocaleDateString()}`}
                      />
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Chip 
                          label={registration.status} 
                          color={getStatusColor(registration.status)}
                          size="small"
                        />
                        {registration.status === 'PENDING' && (
                          <>
                            <Button 
                              size="small" 
                              color="success"
                              variant="outlined"
                              onClick={() => handleUpdateRegistrationStatus(registration.id, 'APPROVED')}
                            >
                              Approve
                            </Button>
                            <Button 
                              size="small" 
                              color="error"
                              variant="outlined"
                              onClick={() => handleUpdateRegistrationStatus(registration.id, 'REJECTED')}
                            >
                              Reject
                            </Button>
                          </>
                        )}
                      </Box>
                    </ListItem>
                    <Divider />
                  </React.Fragment>
                ))}
              </List>
            )}
          </CardContent>
        </Card>
      )}

      {tabValue === 2 && !isNewTournament && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>Tournament Matches</Typography>
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
              <Button 
                variant="contained" 
                color="primary"
                onClick={() => navigate('/admin/matches/new', { state: { tournamentId: id } })}
              >
                Add Match
              </Button>
            </Box>
            
            {matches.length === 0 ? (
              <Typography variant="body1" color="textSecondary">
                No matches found for this tournament.
              </Typography>
            ) : (
              <List>
                {matches.map((match) => (
                  <React.Fragment key={match.id}>
                    <ListItem
                      button
                      onClick={() => navigate(`/admin/matches/${match.id}`)}
                    >
                      <ListItemText
                        primary={`${match.player1Name} vs ${match.player2Name}`}
                        secondary={`Referee: ${match.refereeName} - Court: ${match.courtNumber} - ${new Date(match.scheduledTime).toLocaleString()}`}
                      />
                      <Chip 
                        label={match.status} 
                        color={getMatchStatusColor(match.status)}
                        size="small"
                      />
                    </ListItem>
                    <Divider />
                  </React.Fragment>
                ))}
              </List>
            )}
          </CardContent>
        </Card>
      )}
    </Paper>
  );
};

export default TournamentDetails;