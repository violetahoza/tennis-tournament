# 🎾 Tennis Tournament Management System

A full-stack web application built to streamline the organization and management of tennis tournaments. The system offers role-based access for players, referees, and administrators, ensuring a tailored and efficient user experience for each participant.

---

## 🎾 Features

### User Management
- **Role-based authorization**: Different interfaces and permissions for administrators, players, and referees
- **Profile management**: Users can update personal information and preferences
- **Secure authentication**: JWT-based authentication with password encryption

### Tournament Management
- **Tournament creation and scheduling**: Set up tournaments with registration deadlines and participant limits
- **Player registration**: Players can register for tournaments with approval workflow
- **Waitlist management**: Automatically promote waitlisted players when spots become available

### Match Management
- **Match scheduling**: Create and assign matches to courts with specific times
- **Referee assignment**: Assign referees to matches with automatic notifications
- **Match scoring**: Real-time score tracking following tennis scoring rules
- **Match status tracking**: Monitor matches from scheduled to completed states

### Notifications
- **Real-time notifications**: In-app notifications for all users
- **Email notifications**: Automated emails for player's registrations status, match scheduling, cancellations, and status changes
- **WebSocket integration**: Instant updates for score changes and match status

### Reporting
- **Match reports**: Generate CSV and TXT reports for matches

## 🏗️ Architecture

The application follows a modern, scalable architecture:

### Frontend
- **React.js**: Component-based UI with React Router for navigation
- **Material UI**: Responsive design with Material Design components
- **Context API**: State management for authentication and user data
- **Axios**: API communication with interceptors for token management

### Backend
- **Spring Boot**: Core framework for REST API development
- **Spring Security**: Authentication and authorization with JWT
- **JPA/Hibernate**: Object-relational mapping for database access
- **WebSocket**: STOMP over SockJS for real-time communication

### Design Patterns
- **Builder Pattern**: For complex object construction (User, Match, Tournament)
- **Observer Pattern**: For event notification (match score updates, registration changes)
- **Repository Pattern**: For data access abstraction
- **MVC Pattern**: For structured API endpoints and controller logic

---

### 🔧 Prerequisites

- Node.js & npm
- Java 17+
- Maven
- MySQL Server

