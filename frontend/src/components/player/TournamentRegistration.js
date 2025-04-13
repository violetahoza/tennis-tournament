import React, { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  Paper, Button, Typography, Box, Divider, Alert, CircularProgress,
  Grid, Chip, List, ListItem, ListItemText, Card, CardContent,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions
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
  const [openCancelDialog, setOpenCancelDialog] = useState(false);
  const [participantsCount, setParticipantsCount] = useState(0);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [tournamentData, registrationData] = await Promise.all([
          fetchTournament(),
          fetchRegistration()
        ]);
        
        // Fetch participants count after getting tournament data
        if (tournamentData) {
          const countRes = await axios.get(
            API_ENDPOINTS.TOURNAMENTS.GET_BY_ID(tournamentData.id) + '/participants-count'
          );
          setParticipantsCount(countRes.data.count || 0);
        }
        
        setLoading(false);
      } catch (error) {
        console.error('Error fetching data:', error);
        setError('An error occurred while fetching data.');
        setLoading(false);
      }
    };
    
    fetchData();
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
      const res = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_PLAYER(auth.user.id));
      
      if (res.data && Array.isArray(res.data)) {
        const tournamentRegistration = res.data.find(reg => reg.tournamentId === parseInt(id));
        
        if (tournamentRegistration) {
          setRegistration(tournamentRegistration);
          return tournamentRegistration;
        }
      }
      return null;
    } catch (err) {
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

  const handleOpenCancelDialog = () => {
    setOpenCancelDialog(true);
  };

  const handleCloseCancelDialog = () => {
    setOpenCancelDialog(false);
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
      setOpenCancelDialog(false);
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
      case 'WAITLISTED': return 'info';
      default: return 'default';
    }
  };

  // Only check if tournament has started for cancellation
  const hasTournamentStarted = () => {
    if (!tournament || !tournament.startDate) return false;
    const today = new Date();
    const startDate = new Date(tournament.startDate);
    return startDate <= today;
  };

  // Allow cancellation for any status as long as tournament hasn't started
  const canCancelRegistration = () => {
    if (!registration) return false;
    if (hasTournamentStarted()) return false;
    return true;
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
          Tournament Details
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
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Chip 
                label={isRegistrationOpen ? "Registration Open" : "Registration Closed"}
                color={isRegistrationOpen ? "success" : "error"}
                size="small"
              />
              {hasTournamentStarted() && (
                <Chip 
                  label="Tournament Started"
                  color="primary"
                  size="small"
                />
              )}
            </Box>
          </Box>
          
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Typography variant="body1" gutterBottom><strong>Location:</strong> {tournament.location}</Typography>
              <Typography variant="body1" gutterBottom><strong>Start Date:</strong> {formatDate(tournament.startDate)}</Typography>
              <Typography variant="body1" gutterBottom><strong>End Date:</strong> {formatDate(tournament.endDate)}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="body1" gutterBottom><strong>Registration Deadline:</strong> {formatDate(tournament.registrationDeadline)}</Typography>
              <Typography variant="body1" gutterBottom><strong>Participants:</strong> {participantsCount || 0}/{tournament.maxParticipants}</Typography>
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
            <Typography variant="h6" gutterBottom>Your Registration Details</Typography>
            
            <List>
              <ListItem divider>
                <ListItemText 
                  primary="Status" 
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
            
            {canCancelRegistration() && (
              <Box sx={{ mt: 3, textAlign: 'center' }}>
                <Button
                  variant="outlined"
                  color="error"
                  onClick={handleOpenCancelDialog}
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
                  {!hasTournamentStarted() && " You can cancel your registration if needed."}
                </Typography>
                {hasTournamentStarted() && (
                  <Typography variant="body2" sx={{ mt: 1 }}>
                    The tournament has already started. If you need to withdraw, please contact the tournament administrators.
                  </Typography>
                )}
              </Alert>
            )}
            
            {registration.status === 'REJECTED' && (
              <Alert severity="error" sx={{ mt: 3 }}>
                <Typography variant="body1">
                  Your registration has been rejected. This could be due to the tournament being full or other eligibility criteria.
                </Typography>
              </Alert>
            )}

            {registration.status === 'WAITLISTED' && (
              <Alert severity="info" sx={{ mt: 3 }}>
                <Typography variant="body1">
                  You are currently on the waitlist for this tournament. You'll be automatically moved to the approved list if a spot becomes available.
                  {!hasTournamentStarted() && " You can cancel your registration if you're no longer interested."}
                </Typography>
              </Alert>
            )}
            
            {registration.status === 'PENDING' && (
              <Alert severity="warning" sx={{ mt: 3 }}>
                <Typography variant="body1">
                  Your registration is pending approval by tournament administrators.
                  {!hasTournamentStarted() && " You can cancel your registration if needed."}
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

      <Dialog
        open={openCancelDialog}
        onClose={handleCloseCancelDialog}
      >
        <DialogTitle>Confirm Cancellation</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {registration?.status === 'APPROVED' 
              ? 'Are you sure you want to cancel your approved registration? Your spot will be given to another player if there is a waitlist.'
              : 'Are you sure you want to cancel your registration?'}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseCancelDialog}>No, Keep My Registration</Button>
          <Button 
            onClick={handleCancelRegistration}
            color="error"
            disabled={submitting}
          >
            Yes, Cancel Registration
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
};

export default TournamentRegistration;