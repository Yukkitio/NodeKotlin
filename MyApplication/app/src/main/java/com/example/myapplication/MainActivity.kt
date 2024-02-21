package com.example.myapplication

// Importation des bibliothèques nécessaires
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter

class MainActivity : AppCompatActivity() {

    // Déclaration des variables pour stocker les références aux composants de l'UI et au Socket
    private lateinit var mSocket: Socket
    private lateinit var slider: SeekBar
    private lateinit var valueSliderText: TextView
    private lateinit var userIdValue: TextView
    private lateinit var logText: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var indicatorImageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var logLinearLayout: LinearLayout
    private lateinit var logContentLinearLayout: LinearLayout

    private var isConnected = false // Pour suivre si l'app est connectée ou non
    private var isServerUpdate = false // Pour savoir si une mise à jour provient du serveur

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Charger le layout de l'activité

        // Initialisation des éléments de l'interface utilisateur
        slider = findViewById(R.id.slider)
        valueSliderText = findViewById(R.id.valueSliderText)
        userIdValue = findViewById(R.id.valueUserIdText)
        logText = findViewById(R.id.logText)
        logScrollView = findViewById(R.id.logScrollView)
        indicatorImageView = findViewById(R.id.indicatorImageView)
        progressBar = findViewById(R.id.connectionProgressBar)
        logLinearLayout = findViewById(R.id.logLinearLayout)
        logContentLinearLayout = findViewById(R.id.logContentLinearLayout)

        // Met à jour la couleur du fond pour indiquer la déconnexion par défaut
        updateIndicatorColor(R.color.disconnected_background)

        // Initialisation du socket
        initSocket()

        // Configuration des événements pour la SeekBar
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Vérifie si la mise à jour provient du serveur
                if (isServerUpdate) {
                    isServerUpdate = false // Réinitialise le flag
                    return
                }
                // Si connecté, informe le serveur du changement
                if (isConnected) {
                    mSocket.emit("sliderValueChanged", progress)
                }
                // Met à jour le texte d'affichage de la valeur
                valueSliderText.text = "Valeur : $progress"
                // Ajoute un log
                addLog("Valeur de la SeekBar modifiée : $progress", Color.WHITE)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        logLinearLayout.setOnClickListener {
            val isOpen = logContentLinearLayout.visibility == View.VISIBLE
            toggleCardView(logContentLinearLayout, isOpen)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initSocket() {
        try {
            // Tente de créer un nouveau socket
            mSocket = IO.socket("http://192.168.1.96:5000")
            // Log la tentative de connexion
            addLog("Connexion au serveur en cours...")
            updateUI(isConnected) // Met à jour l'UI en fonction de l'état de la connexion
        } catch (e: Exception) {
            // Log une erreur si la connexion échoue
            addLog("Erreur d'initialisation du socket : $e")
        }

        // Initialisation des écouteurs pour les événements serveur et données
        initServerEvents()
        initDataEvents()

        // Établit la connexion
        mSocket.connect()
    }

    // Initialise les écouteurs pour les événements liés au serveur (connexion, déconnexion, etc.)
    private fun initServerEvents() {
        // À la connexion
        mSocket.on(Socket.EVENT_CONNECT, Emitter.Listener {
            isConnected = true
            updateUI(isConnected)
            updateIndicatorColor(R.color.connected_background)
            addLog(" -- Connecté au serveur ! -- ", Color.GREEN)
        })

        // En cas d'erreur de connexion
        mSocket.on(Socket.EVENT_CONNECT_ERROR, Emitter.Listener { args ->
            isConnected = false
            updateUI(isConnected)
            updateIndicatorColor(R.color.error_background)
            addLog("Erreur : ${args[0]}", Color.RED)
        })

        // À la déconnexion
        mSocket.on(Socket.EVENT_DISCONNECT, Emitter.Listener {
            isConnected = false
            updateUI(isConnected)
            updateIndicatorColor(R.color.disconnected_background)
            addLog(" -- Déconnecté du serveur ! -- ", Color.RED)
        })
    }

    // Initialise les écouteurs pour les événements liés aux données (changement de valeur, etc.)
    @SuppressLint("SetTextI18n")
    private fun initDataEvents() {
        // Écouteur pour l'ID utilisateur
        mSocket.on("userId", Emitter.Listener { args ->
            val userId = args[0] as String // Extrait l'ID utilisateur
            userIdValue.text = "ID : $userId" // Met à jour l'affichage
            addLog("ID utilisateur : $userId", Color.WHITE) // Log l'événement
        })

        // À la réception de la valeur de la SeekBar
        mSocket.on("sliderValueChanged", Emitter.Listener { args ->
            val newValue = when (val arg = args[0]) {
                is String -> arg.toIntOrNull()
                is Int -> arg
                else -> null
            } ?: return@Listener

            runOnUiThread {
                isServerUpdate = true
                slider.progress = newValue
                valueSliderText.text = "Valeur : $newValue"
                addLog("Valeur de la SeekBar mise à jour à partir d'un autre client : $newValue", Color.MAGENTA)
            }
        })
    }

    // Fonction pour mettre à jour l'UI en fonction de l'état de la connexion
    private fun updateUI(isConnected: Boolean) {
        runOnUiThread {
            if (isConnected) {
                // Affichez la SeekBar et le texte
                slider.visibility = View.VISIBLE
                valueSliderText.visibility = View.VISIBLE
                // Cachez la ProgressBar
                progressBar.visibility = View.INVISIBLE
            } else {
                // Cachez la SeekBar et le texte
                slider.visibility = View.INVISIBLE
                valueSliderText.visibility = View.INVISIBLE
                // Affichez la ProgressBar
                progressBar.visibility = View.VISIBLE
            }
        }
    }

    // Fonction pour ajouter un log à l'interface utilisateur
    private fun addLog(message: String, color: Int = Color.WHITE) {
        runOnUI {
            val coloredMessage = SpannableString("$message\n")
            coloredMessage.setSpan(ForegroundColorSpan(color), 0, coloredMessage.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            logText.append(coloredMessage)
            logScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    // Fonction pour changer la couleur du fond de l'indicateur de connexion
    private fun updateIndicatorColor(colorResId: Int) {
        runOnUI {
            indicatorImageView.setColorFilter(ContextCompat.getColor(this, colorResId))
        }
    }

    // Fonction pour changer l'affichage de la Card console log
    private fun toggleCardView(view: View, isOpen: Boolean) {
        // Mesurer la vue si elle n'est pas ouverte
        if (!isOpen) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec((view.parent as ViewGroup).width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        // Obtenir les hauteurs de départ et de fin
        val startHeight = if (isOpen) view.height else 0
        val endHeight = if (isOpen) 0 else view.measuredHeight

        // Créer et configurer l'animateur
        ValueAnimator.ofInt(startHeight, endHeight).apply {
            duration = 375 // Durée de l'animation en ms

            // Mettre à jour la hauteur pendant l'animation
            addUpdateListener {
                view.layoutParams.height = it.animatedValue as Int
                view.requestLayout()
            }

            // Actions à faire au début et à la fin de l'animation
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    if (!isOpen) view.visibility = View.VISIBLE
                }
                override fun onAnimationEnd(animation: Animator) {
                    if (isOpen) view.visibility = View.GONE
                }
            })

            // Démarrer l'animation
            start()
        }
    }

    // Fonction utilitaire pour exécuter des actions sur le thread UI
    private fun runOnUI(action: () -> Unit) {
        if (isMainThread()) {
            action()
        } else {
            runOnUiThread(action)
        }
    }

    // Fonction utilitaire pour vérifier si le code est en cours d'exécution sur le thread UI principal
    private fun isMainThread(): Boolean = Thread.currentThread() == mainLooper.thread

    override fun onDestroy() {
        super.onDestroy()
        mSocket.disconnect() // Assurez-vous de déconnecter le socket lors de la destruction de l'activité
    }
}
