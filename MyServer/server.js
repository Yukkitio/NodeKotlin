const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');

// Initialisation de l'application Express
const app = express();

// Création du serveur HTTP en utilisant l'application Express
const server = http.createServer(app);

// Configuration de Socket.IO avec les options CORS
const io = new Server(server, {
  cors: {
    origin: '*', // Autoriser toutes les origines
    methods: ['GET', 'POST'],
    credentials: true,
  },
});

// Écouteur d'événement pour gérer les connexions des utilisateurs
io.on('connection', (socket) => {
  console.log('Nouvelle connexion :', socket.id);

  // Envoyer l'ID de l'utilisateur connecté à celui-ci
  socket.emit('userId', socket.id);

  // Écouteur d'événements pour recevoir des données de l'application Android
  socket.on('sliderValueChanged', (data) => {
    // console.log(`Slider Value Changed - Utilisateur ${socket.id}: ${data}`);
    // Envoyer cette valeur à tous les autres clients connectés (sauf l'émétteur)
    socket.broadcast.emit('sliderValueChanged', data);
  });

  // Écouteur d'événement pour gérer la déconnexion de l'utilisateur
  socket.on('disconnect', () => {
    console.log(`Utilisateur ${socket.id} déconnecté`);
  });
});

// Définition du port d'écoute du serveur
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`Le serveur fonctionne sur le port ${PORT}`);
});

app.get('/', (req, res) => {
  res.send('<script src="/socket.io/socket.io.js"></script> <script> var socket = io(); </script>');
});


// NE PASS OUBLIEZ DESACTIVER PARE-FEU !