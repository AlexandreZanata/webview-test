package br.com.alexandre

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest // NOVO
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var offlineLayout: ConstraintLayout
    private lateinit var offlineText: TextView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null
    private var locationManager: LocationManager? = null
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    // NOVO: Variável para armazenar a solicitação de permissão pendente do WebView
    private var pendingPermissionRequest: PermissionRequest? = null

    // NOVO: Constantes para o canal de notificação
    private val CHANNEL_ID = "br.com.alexandre.NOTIFICATION_CHANNEL"
    private val CHANNEL_NAME = "Notificações do Aplicativo"
    private val CHANNEL_DESCRIPTION = "Notificações importantes do sistema"

    private val URL_TO_LOAD = "https://i9gestao-sistemas.com.br/diario_frota/"

    // Códigos de requisição
    private val FILECHOOSER_RESULTCODE = 1
    private val CAMERA_REQUEST_CODE = 2
    private val LOCATION_PERMISSION_CODE = 3
    private val NOTIFICATION_PERMISSION_CODE = 4
    private val AUDIO_PERMISSION_REQUEST_CODE = 5 // NOVO

    // Download ID
    private var downloadID: Long = -1

    // Permissões necessárias
    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.RECORD_AUDIO // NOVO
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO // NOVO
        )
    }

    // Receptor para monitorar alterações de conectividade
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // NOVO: Criar o canal de notificação
            createNotificationChannel()

            // Inicializando views
            try {
                webView = findViewById(R.id.webView)
                progressBar = findViewById(R.id.progressBar)
                offlineLayout = findViewById(R.id.offlineView)
                offlineText = findViewById(R.id.offlineText)
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao inicializar views", e)
            }

            // Configurar WebView
            try {
                setupWebView()
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao configurar WebView", e)
            }

            // Configurar callback de rede
            try {
                setupNetworkCallback()
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao configurar network callback", e)
            }

            // Verificar permissões
            try {
                checkAndRequestPermissions()
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao verificar permissões", e)
            }

            // Registrar o receiver para download
            try {
                registerDownloadReceiver()
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao registrar download receiver", e)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro crítico no onCreate", e)
        }
    }

    // NOVO: Método para criar o canal de notificação
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d("Notification", "Canal de notificação criado")
        }
    }

    private fun registerDownloadReceiver() {
        try {
            // Create and register the download receiver
            val downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        val action = intent?.action
                        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                            if (downloadId == downloadID) {
                                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                val query = DownloadManager.Query().setFilterById(downloadId)
                                val cursor = downloadManager.query(query)

                                if (cursor.moveToFirst()) {
                                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                    if (columnIndex >= 0) {
                                        val status = cursor.getInt(columnIndex)
                                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                            Toast.makeText(context, "Download concluído com sucesso", Toast.LENGTH_SHORT).show()
                                        } else if (status == DownloadManager.STATUS_FAILED) {
                                            Toast.makeText(context, "Falha no download", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                cursor.close()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DownloadReceiver", "Erro ao processar download", e)
                    }
                }
            }
            registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        } catch (e: Exception) {
            Log.e("DownloadReceiver", "Erro ao registrar receiver", e)
        }
    }

    private fun setupNetworkCallback() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // Executado na thread principal para atualizar a UI
                    runOnUiThread {
                        try {
                            Log.d("NetworkCallback", "Conexão disponível")
                            offlineLayout.visibility = View.GONE
                            webView.visibility = View.VISIBLE

                            if (webView.url == null || webView.url!!.isEmpty()) {
                                webView.loadUrl(URL_TO_LOAD)
                            }
                        } catch (e: Exception) {
                            Log.e("NetworkCallback", "Erro no callback onAvailable", e)
                        }
                    }
                }

                override fun onLost(network: Network) {
                    // Executado na thread principal para atualizar a UI
                    runOnUiThread {
                        try {
                            Log.d("NetworkCallback", "Conexão perdida")
                            webView.visibility = View.GONE
                            offlineLayout.visibility = View.VISIBLE
                        } catch (e: Exception) {
                            Log.e("NetworkCallback", "Erro no callback onLost", e)
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, networkCallback)
            }

            // Verificação inicial de conectividade
            checkInternetConnection()
        } catch (e: Exception) {
            Log.e("NetworkCallback", "Erro ao configurar network callback", e)
        }
    }

    private fun checkInternetConnection() {
        try {
            if (isNetworkAvailable()) {
                offlineLayout.visibility = View.GONE
                webView.visibility = View.VISIBLE

                if (webView.url == null || webView.url!!.isEmpty()) {
                    webView.loadUrl(URL_TO_LOAD)
                }
            } else {
                webView.visibility = View.GONE
                offlineLayout.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e("Internet", "Erro ao verificar conexão", e)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

                return when {
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                return networkInfo != null && networkInfo.isConnected
            }
        } catch (e: Exception) {
            Log.e("Network", "Erro ao verificar disponibilidade de rede", e)
            return false
        }
    }

    private fun checkAndRequestPermissions() {
        try {
            val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest, LOCATION_PERMISSION_CODE)
            } else {
                // Todas as permissões já concedidas
                startLocationUpdates()
            }

            // Verificar especificamente a permissão de notificações
            checkNotificationPermission()
        } catch (e: Exception) {
            Log.e("Permissions", "Erro ao verificar permissões", e)
        }
    }

    private fun checkNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_CODE
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("Permissions", "Erro ao verificar permissão de notificação", e)
        }
    }

    private fun startLocationUpdates() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

                // Tentar obter localização do GPS
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000,  // intervalo mínimo (ms)
                        10f,   // distância mínima (m)
                        this
                    )
                } catch (ex: Exception) {
                    Log.e("Location", "Erro ao solicitar atualizações de GPS: ${ex.message}")
                }

                // Tentar também localização baseada em rede como backup
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        10f,
                        this
                    )
                } catch (ex: Exception) {
                    Log.e("Location", "Erro ao solicitar atualizações de rede: ${ex.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("Location", "Erro ao iniciar atualizações de localização", e)
        }
    }

    override fun onLocationChanged(location: Location) {
        try {
            userLatitude = location.latitude
            userLongitude = location.longitude
            Log.d("Location", "Localização atual: $userLatitude, $userLongitude")

            // Passar a localização para a WebView
            webView.evaluateJavascript(
                "javascript:if(typeof updateLocation === 'function'){updateLocation($userLatitude, $userLongitude);}",
                null
            )
        } catch (e: Exception) {
            Log.e("Location", "Erro ao processar mudança de localização", e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        try {
            // Configurações básicas
            with(webView.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true

                // Caching
                cacheMode = WebSettings.LOAD_DEFAULT

                // Configurações do DOM Storage
                databaseEnabled = true

                // Configurações de UX
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false

                // Viewport para comportamento responsivo
                useWideViewPort = true
                loadWithOverviewMode = true

                // Configuração para upload de arquivos
                allowFileAccess = true

                // Otimizações para carregamento mais rápido
                loadsImagesAutomatically = true

                // Configurações para permitir download de arquivos
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                allowContentAccess = true

                // Permitir JavaScript interfaces
                javaScriptCanOpenWindowsAutomatically = true

                // NOVO: Necessário para gravação/reprodução de mídia sem interação direta do usuário
                mediaPlaybackRequiresUserGesture = false
            }

            // Adicionar interface JavaScript para captura de tela e PDF
            webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

            // Configuração de cookies
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            }

            // Habilitar aceleração de hardware
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

            // WebViewClient com verificação de conectividade e correção para manter links internos
            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.visibility = View.GONE
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)

                    // Verificar se o erro é relacionado à rede principal (não recursos secundários)
                    if (request?.isForMainFrame == true) {
                        Log.e("WebView", "Erro ao carregar página: ${error?.description}")
                        // Verificar conexão novamente
                        checkInternetConnection()
                    }
                }

                // CORREÇÃO: Importante para manter navegação dentro do app
                @SuppressLint("ObsoleteSdkInt")
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val url = request?.url?.toString() ?: ""
                        Log.d("WebViewNav", "Navegando para: $url")
                        return handleUrl(url, view)
                    }
                    return false
                }

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    Log.d("WebViewNav", "Navegando (deprecated) para: $url")
                    return handleUrl(url ?: "", view)
                }

                // CORREÇÃO: Esta função foi revisada para manter links dentro do app
                private fun handleUrl(url: String, view: WebView?): Boolean {
                    try {
                        Log.d("WebViewNav", "Avaliando URL: $url")

                        // Verificar se é um link para download de arquivo
                        if (url.startsWith("http") && isFileUrl(url)) {
                            Log.d("WebViewNav", "URL é arquivo para download")
                            downloadFile(url, webView.settings.userAgentString, "", getMimeType(url))
                            return true
                        }

                        // CORREÇÃO: Para URLs HTTP/HTTPS, sempre carregar dentro do WebView
                        // exceto para arquivos que devem ser baixados
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            Log.d("WebViewNav", "Carregando no WebView: $url")
                            view?.loadUrl(url)
                            return true
                        }

                        // Para esquemas especiais (não http/https) como mailto:, tel:, etc.
                        if (!url.startsWith("http")) {
                            try {
                                Log.d("WebViewNav", "Abrindo esquema especial: $url")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                Log.e("WebViewNav", "Erro ao abrir URL especial: $url", e)
                                return true
                            }
                        }

                        // Em caso de dúvida, deixar o WebView lidar com a URL
                        return false
                    } catch (e: Exception) {
                        Log.e("WebViewNav", "Erro ao processar URL: $url", e)
                        return false
                    }
                }

                private fun isFileUrl(url: String): Boolean {
                    val lowerCaseUrl = url.lowercase(Locale.ROOT)
                    return lowerCaseUrl.endsWith(".pdf") ||
                            lowerCaseUrl.endsWith(".csv") ||
                            lowerCaseUrl.endsWith(".xls") ||
                            lowerCaseUrl.endsWith(".xlsx")
                }

                private fun getMimeType(url: String): String {
                    val lowerCaseUrl = url.lowercase(Locale.ROOT)
                    return when {
                        lowerCaseUrl.endsWith(".pdf") -> "application/pdf"
                        lowerCaseUrl.endsWith(".csv") -> "text/csv"
                        lowerCaseUrl.endsWith(".xls") -> "application/vnd.ms-excel"
                        lowerCaseUrl.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        else -> "application/octet-stream"
                    }
                }
            }

            // WebChromeClient com suporte a upload de arquivos e permissões de áudio
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progressBar.progress = newProgress
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (this@MainActivity.filePathCallback != null) {
                        this@MainActivity.filePathCallback?.onReceiveValue(null)
                    }
                    this@MainActivity.filePathCallback = filePathCallback

                    checkUploadPermissions()
                    return true
                }

                // NOVO: Lida com solicitações de permissão do site (como microfone)
                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (request == null) return

                    // Verificar se a solicitação é para captura de áudio
                    if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                        // Verificar se já temos a permissão de gravação de áudio
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            // Se a permissão já foi concedida, autoriza o WebView
                            request.grant(request.resources)
                        } else {
                            // Se não, armazena a solicitação e pede a permissão ao usuário
                            pendingPermissionRequest = request
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(Manifest.permission.RECORD_AUDIO),
                                AUDIO_PERMISSION_REQUEST_CODE
                            )
                        }
                    } else {
                        // Para outras solicitações de permissão, negar ou tratar conforme necessário
                        super.onPermissionRequest(request)
                    }
                }
            }

            // Configurar download listener para permitir download de arquivos PDF, CSV, Excel, etc.
            webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                try {
                    Log.d("DownloadDebug", "URL: $url, MimeType: $mimetype")

                    // Verificar se a URL começa com http ou https
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        Toast.makeText(this, "Apenas downloads HTTP/HTTPS são permitidos", Toast.LENGTH_SHORT).show()
                        return@setDownloadListener
                    }

                    // Verificar se o arquivo é de um tipo permitido
                    if (isFileTypeAllowed(mimetype) || isFileTypeByExtension(url)) {
                        // Iniciar o download do arquivo
                        downloadFile(url, userAgent, contentDisposition, mimetype)
                    } else {
                        Toast.makeText(this, "Tipo de arquivo não suportado", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("DownloadDebug", "Erro no listener de download", e)
                    Toast.makeText(this, "Erro ao processar download: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // Carregar URL inicial
            webView.loadUrl(URL_TO_LOAD)
        } catch (e: Exception) {
            Log.e("WebView", "Erro ao configurar WebView", e)
        }
    }

    // Interface JavaScript para comunicação entre WebView e Android
    inner class WebAppInterface {
        @JavascriptInterface
        fun getLocation(): String {
            return "{\"latitude\": $userLatitude, \"longitude\": $userLongitude}"
        }

        @JavascriptInterface
        fun captureScreenshot() {
            runOnUiThread {
                convertWebViewToPdf()
            }
        }

        @JavascriptInterface
        fun setUserId(id: String) {
            Log.d("WebAppInterface", "ID do usuário definido: $id")
        }

        // NOVO: Método completo para mostrar notificações do sistema
        @JavascriptInterface
        fun showNotification(title: String, message: String, type: String = "info") {
            try {
                Log.d("WebAppInterface", "Notificação: $title, $message, $type")

                // Gerar um ID único para cada notificação
                val notificationId = Random().nextInt(100000)

                // Definir o ícone com base no tipo
                val icon = when(type.lowercase()) {
                    "alert", "warning" -> android.R.drawable.ic_dialog_alert
                    "message" -> android.R.drawable.ic_dialog_email
                    else -> android.R.drawable.ic_dialog_info
                }

                // Criar intent para abrir o app quando a notificação for tocada
                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("notification_message", message)
                }

                // Criar PendingIntent
                val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getActivity(
                        this@MainActivity,
                        notificationId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    PendingIntent.getActivity(
                        this@MainActivity,
                        notificationId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

                // Construir a notificação
                val builder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // Adicionar vibração para notificações importantes
                if (type.lowercase() == "alert" || type.lowercase() == "warning") {
                    builder.setVibrate(longArrayOf(0, 500, 200, 500))
                    builder.priority = NotificationCompat.PRIORITY_HIGH
                }

                // Criar a notificação na UI thread
                Handler(Looper.getMainLooper()).post {
                    // Verificar permissões antes de mostrar a notificação
                    val notificationManagerCompat = NotificationManagerCompat.from(this@MainActivity)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED) {
                            notificationManagerCompat.notify(notificationId, builder.build())
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Permissão de notificação necessária",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Solicitar permissão novamente
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                NOTIFICATION_PERMISSION_CODE
                            )
                        }
                    } else {
                        // Para Android < 13, não é necessária permissão especial
                        notificationManagerCompat.notify(notificationId, builder.build())
                    }
                }
            } catch (e: Exception) {
                Log.e("Notification", "Erro ao criar notificação", e)
            }
        }
    }

    private fun convertWebViewToPdf() {
        try {
            // Verificar se temos permissão para escrever no armazenamento
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissão de armazenamento necessária", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            // Obtém dimensões da WebView
            val webViewWidth = webView.width
            val webViewHeight = webView.contentHeight

            // Cria bitmap para armazenar a captura
            val bitmap = Bitmap.createBitmap(webViewWidth, webViewHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)

            // Cria documento PDF
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(webViewWidth, webViewHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            // Desenha o bitmap na página
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)

            // Nome do arquivo com timestamp
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "webpage_$timeStamp.pdf"

            // Salva o arquivo
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Para Android 10+ usamos MediaStore
                    val contentValues = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }

                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        val outputStream = contentResolver.openOutputStream(uri)
                        if (outputStream != null) {
                            pdfDocument.writeTo(outputStream)
                            outputStream.close()
                            Toast.makeText(this, "PDF salvo em Downloads/$fileName", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Para versões anteriores usamos o diretório de downloads diretamente
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    val pdfFile = File(downloadsDir, fileName)
                    val outputStream = FileOutputStream(pdfFile)
                    pdfDocument.writeTo(outputStream)
                    outputStream.close()
                    Toast.makeText(this, "PDF salvo em Downloads/$fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PDF", "Erro ao salvar PDF", e)
                Toast.makeText(this, "Erro ao salvar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }

            // Fecha o documento
            pdfDocument.close()

        } catch (e: Exception) {
            Log.e("PDF", "Erro ao criar PDF", e)
            Toast.makeText(this, "Erro ao criar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUploadPermissions() {
        try {
            val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (cameraPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED) {
                showOptionsDialog()
            } else {
                val permissionsToRequest = ArrayList<String>()

                if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.CAMERA)
                }

                if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                    } else {
                        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }

                if (permissionsToRequest.isNotEmpty()) {
                    ActivityCompat.requestPermissions(
                        this,
                        permissionsToRequest.toTypedArray(),
                        100
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("Permissions", "Erro ao verificar permissões para upload", e)
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    private fun isFileTypeByExtension(url: String): Boolean {
        val lowerCaseUrl = url.lowercase(Locale.ROOT)
        return lowerCaseUrl.endsWith(".pdf") ||
                lowerCaseUrl.endsWith(".csv") ||
                lowerCaseUrl.endsWith(".xls") ||
                lowerCaseUrl.endsWith(".xlsx")
    }

    private fun isFileTypeAllowed(mimetype: String): Boolean {
        // Lista de tipos MIME permitidos
        val allowedMimeTypes = listOf(
            "application/pdf", // PDF
            "application/vnd.ms-excel", // Excel antigo (.xls)
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // Excel moderno (.xlsx)
            "text/csv", // CSV
            "application/csv", // CSV alternativo
            "text/comma-separated-values", // CSV alternativo
            "application/octet-stream" // Tipo genérico (verificaremos a extensão)
        )

        return allowedMimeTypes.any { mimetype.equals(it, ignoreCase = true) }
    }

    private fun downloadFile(url: String, userAgent: String, contentDisposition: String, mimetype: String) {
        try {
            // Garantir que a URL começa com http/https
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(this, "Apenas downloads HTTP/HTTPS são permitidos", Toast.LENGTH_SHORT).show()
                return
            }

            // Verificar permissão de armazenamento para versões antigas do Android
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1001
                    )
                    return
                }
            }

            val request = DownloadManager.Request(Uri.parse(url))

            // Log para debug
            Log.d("DownloadDebug", "Iniciando download: $url")
            Log.d("DownloadDebug", "MimeType: $mimetype")
            Log.d("DownloadDebug", "ContentDisposition: $contentDisposition")

            // Extrair o nome de arquivo da URL ou do Content-Disposition
            val fileName = if (contentDisposition.isNotEmpty()) {
                URLUtil.guessFileName(url, contentDisposition, mimetype)
            } else {
                // Se não tiver Content-Disposition, extrair da URL
                url.substring(url.lastIndexOf('/') + 1).let { name ->
                    if (name.contains('?')) name.substring(0, name.indexOf('?')) else name
                }
            }

            Log.d("DownloadDebug", "Nome do arquivo: $fileName")

            // Configurar o request
            request.apply {
                setMimeType(mimetype)
                addRequestHeader("User-Agent", userAgent)
                addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                setDescription("Baixando arquivo")
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                // Destino do arquivo
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    val destinationPath = File(downloadsDir, fileName)
                    setDestinationUri(Uri.fromFile(destinationPath))
                }

                // Permitir downloads sobre redes móveis e Wi-Fi
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setRequiresCharging(false)
                    setRequiresDeviceIdle(false)
                }
            }

            // Obter o serviço de download e enfileirar o request
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadID = downloadManager.enqueue(request)

            Toast.makeText(this, "Download iniciado: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("DownloadDebug", "Erro ao iniciar o download", e)
            Toast.makeText(this, "Erro ao iniciar o download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showOptionsDialog() {
        try {
            val options = arrayOf("Câmera", "Galeria")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Selecionar de")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> openCamera()
                        1 -> openGallery()
                    }
                }
                .setOnCancelListener {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                }
                .show()
        } catch (e: Exception) {
            Log.e("Dialog", "Erro ao mostrar diálogo", e)
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                } catch (e: IOException) {
                    Log.e("MainActivity", "Error creating image file", e)
                }

                if (photoFile != null) {
                    cameraImageUri = FileProvider.getUriForFile(
                        this,
                        "br.com.alexandre.fileprovider",
                        photoFile
                    )

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                    try {
                        startActivityForResult(intent, CAMERA_REQUEST_CODE)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error starting camera", e)
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = null
                        cameraImageUri = null
                    }
                } else {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                }
            } else {
                Toast.makeText(this, "Nenhum aplicativo de câmera disponível", Toast.LENGTH_SHORT).show()
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
        } catch (e: Exception) {
            Log.e("Camera", "Erro ao abrir câmera", e)
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    private fun openGallery() {
        try {
            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.type = "image/*"

            startActivityForResult(
                Intent.createChooser(contentSelectionIntent, "Selecionar imagem"),
                FILECHOOSER_RESULTCODE
            )
        } catch (e: Exception) {
            Log.e("Gallery", "Erro ao abrir galeria", e)
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.mkdirs()

        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.main_menu, menu)
        } catch (e: Exception) {
            Log.e("Menu", "Erro ao criar menu", e)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                R.id.action_capture_pdf -> {
                    convertWebViewToPdf()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e("Menu", "Erro ao processar item de menu", e)
            super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data)

            if (filePathCallback == null) {
                return
            }

            if (requestCode == CAMERA_REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    if (cameraImageUri != null) {
                        filePathCallback?.onReceiveValue(arrayOf(cameraImageUri!!))
                    } else {
                        filePathCallback?.onReceiveValue(null)
                    }
                } else {
                    filePathCallback?.onReceiveValue(null)
                }
                filePathCallback = null
                cameraImageUri = null
            } else if (requestCode == FILECHOOSER_RESULTCODE) {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val dataUri = data.data
                    if (dataUri != null) {
                        filePathCallback?.onReceiveValue(arrayOf(dataUri))
                    } else {
                        filePathCallback?.onReceiveValue(null)
                    }
                } else {
                    filePathCallback?.onReceiveValue(null)
                }
                filePathCallback = null
            }
        } catch (e: Exception) {
            Log.e("ActivityResult", "Erro ao processar resultado", e)
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            when (requestCode) {
                100 -> {
                    // Resposta às permissões de upload
                    if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                        showOptionsDialog()
                    } else {
                        // Se apenas permissão de armazenamento foi concedida, podemos abrir a galeria
                        if (permissions.size > 1 &&
                            permissions[0] == Manifest.permission.CAMERA &&
                            permissions[1].contains("STORAGE") &&
                            grantResults.size > 1 &&
                            grantResults[0] != PackageManager.PERMISSION_GRANTED &&
                            grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                            openGallery()
                        } else {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = null
                            Toast.makeText(this, "Permissões negadas", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                LOCATION_PERMISSION_CODE -> {
                    // Resposta às permissões de localização
                    if (grantResults.isNotEmpty() &&
                        (permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) ||
                                permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) &&
                        grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                        // Pelo menos uma permissão de localização concedida
                        startLocationUpdates()
                    }
                }
                NOTIFICATION_PERMISSION_CODE -> {
                    // Resposta à permissão de notificação
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("Notification", "Permissão de notificação concedida")
                        // NOVO: Você pode exibir uma notificação de teste aqui se desejar
                    } else {
                        Log.d("Notification", "Permissão de notificação negada")
                        Toast.makeText(
                            this,
                            "As notificações não serão exibidas sem a permissão",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                1001 -> {
                    // Resposta à permissão de armazenamento para download
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Agora você pode baixar arquivos", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Permissão necessária para baixar arquivos", Toast.LENGTH_SHORT).show()
                    }
                }
                // NOVO: Lida com a resposta da permissão de áudio
                AUDIO_PERMISSION_REQUEST_CODE -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // Permissão concedida pelo usuário, agora podemos concedê-la ao WebView
                        pendingPermissionRequest?.grant(pendingPermissionRequest!!.resources)
                        Toast.makeText(this, "Permissão de áudio concedida.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Permissão negada pelo usuário, negamos no WebView
                        pendingPermissionRequest?.deny()
                        Toast.makeText(this, "Permissão de áudio negada. A gravação não funcionará.", Toast.LENGTH_LONG).show()
                    }
                    // Limpar a solicitação pendente
                    pendingPermissionRequest = null
                }
            }
        } catch (e: Exception) {
            Log.e("Permissions", "Erro ao processar resultado de permissões", e)
        }
    }

    override fun onBackPressed() {
        try {
            if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
        } catch (e: Exception) {
            Log.e("Navigation", "Erro ao processar botão voltar", e)
            super.onBackPressed()
        }
    }

    override fun onResume() {
        try {
            super.onResume()
            webView.onResume()
            checkInternetConnection()

            // Retomar atualizações de localização
            if (locationManager != null && (
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED ||
                                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED)) {
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000,
                        10f,
                        this
                    )
                } catch (ex: Exception) {
                    Log.e("Location", "Erro ao retomar atualizações de localização", ex)
                }
            }
        } catch (e: Exception) {
            Log.e("Lifecycle", "Erro no onResume", e)
        }
    }

    override fun onPause() {
        try {
            webView.onPause()

            // Pausar atualizações de localização
            if (locationManager != null) {
                locationManager?.removeUpdates(this)
            }

            super.onPause()
        } catch (e: Exception) {
            Log.e("Lifecycle", "Erro no onPause", e)
            super.onPause()
        }
    }

    override fun onDestroy() {
        try {
            // Desregistrar o callback de rede
            if (::connectivityManager.isInitialized && ::networkCallback.isInitialized) {
                try {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                } catch (e: Exception) {
                    Log.e("NetworkCallback", "Erro ao desregistrar callback: ${e.message}")
                }
            }

            webView.destroy()
            super.onDestroy()
        } catch (e: Exception) {
            Log.e("Lifecycle", "Erro no onDestroy", e)
            super.onDestroy()
        }
    }
}
