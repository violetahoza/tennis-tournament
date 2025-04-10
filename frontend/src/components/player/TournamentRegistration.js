import React, { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  Paper, Button, Typography, Box, Divider, Alert, CircularProgress,
  Grid, Chip, List, ListItem, ListItemText, Card, CardContent
} from '@mui/material';
import { ArrowBack as ArrowBackIcon, EmojiEvents as TrophyIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';
import { AuthContext } from '../../context/AuthContext';

const TournamentRegistration = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { auth } = useContext(AuthContext);
  
  const [tournament, setTournament] = useState(null);
  const [registration, setRegistration] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    Promise.all([
      fetchTournament(),
      fetchRegistration()
    ]).then(() => {
      setLoading(false);
    }).catch(error => {
      console.error('Error fetching data:', error);
      setError('An error occurred while fetching data.');
      setLoading(false);
    });
  }, [id]);

  const fetchTournament = async () => {
    try {
      const res = await axios.get(API_ENDPOINTS.TOURNAMENTS.GET_BY_ID(id));
      setTournament(res.data);
      return res.data;
    } catch (err) {
      console.error('Error fetching tournament:', err);
      setError('Error fetching tournament details. Please try again.');
      throw err;
    }
  };

  const fetchRegistration = async () => {
    if (!auth.user?.id) return null;
    
    try {
      // We need to query all registrations for this player and find the one for this tournament
      const res = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_PLAYER(auth.user.id));
      
      // Check if we have registrations data
      if (res.data && Array.isArray(res.data)) {
        const tournamentRegistration = res.data.find(reg => reg.tournamentId === parseInt(id));
        
        if (tournamentRegistration) {
          setRegistration(tournamentRegistration);
          return tournamentRegistration;
        }
      }
      return null;
    } catch (err) {
      // Registration might not exist, which is fine
      console.log('No existing registration found:', err);
      return null;
    }
  };

  const handleRegister = async () => {
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    
    try {
      const res = await axios.post(
        API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.REGISTER(auth.user.id, id)
      );
      
      setRegistration(res.data);
      setSuccess('Registration submitted successfully! Your registration is pending approval.');
    } catch (err) {
      setError(err.response?.data?.message || 'Error submitting registration. Please try again.');
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelRegistration = async () => {
    if (!registration) return;
    
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    
    try {
      await axios.delete(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.CANCEL(registration.id));
      setRegistration(null);
      setSuccess('Registration cancelled successfully!');
    } catch (err) {
      setError(err.response?.data?.message || 'Error cancelling registration. Please try again.');
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleBack = () => {
    navigate('/player/tournaments');
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    return new Date(dateString).toLocaleDateString();
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

  if (!tournament) {
    return (
      <Alert severity="error">
        Tournament not found. <Button onClick={handleBack}>Go Back</Button>
      </Alert>
    );
  }

  const isRegistrationOpen = tournament.registrationDeadline && new Date(tournament.registrationDeadline) > new Date();

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
          Tournament Registration
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      <Card sx={{ mb: 4 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <TrophyIcon sx={{ mr: 1, color: 'primary.main' }} />
              <Typography variant="h6">{tournament.name}</Typography>
            </Box>
            <Chip 
              label={isRegistrationOpen ? "Registration Open" : "Registration Closed"}
              color={isRegistrationOpen ? "success" : "error"}
            />
          </Box>
          
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Typography variant="body1" gutterBottom><strong>Location:</strong> {tournament.location}</Typography>
              <Typography variant="body1" gutterBottom><strong>Start Date:</strong> {formatDate(tournament.startDate)}</Typography>
              <Typography variant="body1" gutterBottom><strong>End Date:</strong> {formatDate(tournament.endDate)}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="body1" gutterBottom><strong>Registration Deadline:</strong> {formatDate(tournament.registrationDeadline)}</Typography>
              <Typography variant="body1" gutterBottom><strong>Participants:</strong> {tournament.registeredPlayers || 0}/{tournament.maxParticipants}</Typography>
            </Grid>
          </Grid>
          
          {tournament.description && (
            <>
              <Typography variant="subtitle1" sx={{ mt: 2, fontWeight: 'bold' }}>Description:</Typography>
              <Typography variant="body2" paragraph>
                {tournament.description}
              </Typography>
            </>
          )}
        </CardContent>
      </Card>

      {registration ? (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>Registration Details</Typography>
            
            <List>
              <ListItem divider>
                <ListItemText 
                  primary="Registration Status" 
                  secondary={
                    <Chip 
                      label={registration.status} 
                      color={getRegistrationStatusColor(registration.status)}
                      sx={{ mt: 1 }}
                    />
                  }
                />
              </ListItem>
              
              <ListItem divider>
                <ListItemText 
                  primary="Registration Date" 
                  secondary={formatDate(registration.registrationDate)}
                />
              </ListItem>
            </List>
            
            {registration.status === 'PENDING' && (
              <Box sx={{ mt: 3, textAlign: 'center' }}>
                <Button
                  variant="outlined"
                  color="error"
                  onClick={handleCancelRegistration}
                  disabled={submitting}
                >
                  {submitting ? 'Cancelling...' : 'Cancel Registration'}
                </Button>
              </Box>
            )}
            
            {registration.status === 'APPROVED' && (
              <Alert severity="success" sx={{ mt: 3 }}>
                <Typography variant="body1">
                  Your registration has been approved! You are all set to participate in this tournament.
                </Typography>
              </Alert>
            )}
            
            {registration.status === 'REJECTED' && (
              <Alert severity="error" sx={{ mt: 3 }}>
                <Typography variant="body1">
                  Unfortunately, your registration has been rejected. This could be due to the tournament being full or other eligibility criteria.
                </Typography>
              </Alert>
            )}
          </CardContent>
        </Card>
      ) : (
        isRegistrationOpen ? (
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <Typography variant="body1" paragraph>
                You are not registered for this tournament yet. Would you like to sign up?
              </Typography>
              <Button
                variant="contained"
                color="primary"
                onClick={handleRegister}
                disabled={submitting}
                size="large"
              >
                {submitting ? 'Submitting...' : 'Register Now'}
              </Button>
            </CardContent>
          </Card>
        ) : (
          <Alert severity="info">
            Registration is not available for this tournament anymore.
          </Alert>
        )
      )}
    </Paper>
  );
};

export default TournamentRegistration;