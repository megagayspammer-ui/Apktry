package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ScriptProject
import org.json.JSONArray
import org.json.JSONObject

class ScriptRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("permscript_projects", Context.MODE_PRIVATE)

    fun getInitialProjects(): List<ScriptProject> {
        val savedJson = prefs.getString("saved_projects_v1", null)
        if (savedJson != null) {
            try {
                val list = mutableListOf<ScriptProject>()
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val permsArray = obj.getJSONArray("requiredPermissions")
                    val perms = mutableListOf<String>()
                    for (j in 0 until permsArray.length()) perms.add(permsArray.getString(j))

                    list.add(
                        ScriptProject(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.getString("description"),
                            iconName = obj.optString("iconName", "code"),
                            requiredPermissions = perms,
                            htmlCode = obj.getString("htmlCode"),
                            lastModified = obj.optLong("lastModified", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return getDefaultTemplates()
    }

    fun saveProjects(projects: List<ScriptProject>) {
        val array = JSONArray()
        for (p in projects) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description)
                put("iconName", p.iconName)
                put("requiredPermissions", JSONArray(p.requiredPermissions))
                put("htmlCode", p.htmlCode)
                put("lastModified", p.lastModified)
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_projects_v1", array.toString()).apply()
    }

    fun resetToDefaults(): List<ScriptProject> {
        val defaults = getDefaultTemplates()
        saveProjects(defaults)
        return defaults
    }

    fun getDefaultTemplates(): List<ScriptProject> {
        return listOf(
            ScriptProject(
                id = "device_toolkit",
                name = "Hardware & Device Toolkit",
                description = "Flashlight, Battery status, Haptic pulses, Text-to-Speech synthesizer, and Clipboard",
                iconName = "hardware",
                requiredPermissions = listOf("CAMERA", "VIBRATE"),
                htmlCode = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <style>
    :root {
      --bg: #0d1117; --surface: #161b22; --border: #30363d;
      --accent: #38bdf8; --accent-glow: rgba(56,189,248,0.25);
      --text: #f0f6fc; --text-muted: #8b949e; --green: #34d399; --pink: #f472b6;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: var(--bg); color: var(--text); padding: 18px;
    }
    .header { margin-bottom: 20px; }
    .badge {
      display: inline-block; padding: 3px 8px; border-radius: 6px;
      font-size: 11px; font-weight: 600; text-transform: uppercase;
      background: var(--accent-glow); color: var(--accent); border: 1px solid var(--accent);
    }
    h1 { font-size: 22px; font-weight: 700; margin: 8px 0 4px; }
    p.sub { font-size: 13px; color: var(--text-muted); }
    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
    .card {
      background: var(--surface); border: 1px solid var(--border);
      border-radius: 12px; padding: 14px;
    }
    .card-full { grid-column: span 2; }
    .stat-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; }
    .stat-val { font-size: 18px; font-weight: 700; margin-top: 4px; color: var(--accent); }
    .btn {
      display: block; width: 100%; border: none; border-radius: 8px;
      padding: 10px 14px; font-size: 13px; font-weight: 600; cursor: pointer;
      background: #21262d; color: var(--text); border: 1px solid var(--border);
      margin-top: 10px; transition: all 0.15s;
    }
    .btn:active { transform: scale(0.98); background: #30363d; }
    .btn-primary { background: var(--accent); color: #000; border-color: var(--accent); }
    .btn-pink { background: var(--pink); color: #000; border-color: var(--pink); }
    input[type="text"] {
      width: 100%; background: #0b0f14; border: 1px solid var(--border);
      color: var(--text); padding: 10px 12px; border-radius: 8px;
      margin-top: 8px; font-size: 13px;
    }
  </style>
</head>
<body>
  <div class="header">
    <span class="badge">Hardware Studio</span>
    <h1>Device & Sensors</h1>
    <p class="sub">Direct native hardware control without compiling or signing</p>
  </div>

  <div class="grid">
    <div class="card">
      <div class="stat-label">Flashlight / Torch</div>
      <div class="stat-val" id="torchStatus">OFF</div>
      <button class="btn btn-primary" onclick="toggleTorch()">Toggle LED Torch</button>
    </div>

    <div class="card">
      <div class="stat-label">Haptic Vibration</div>
      <div class="stat-val">Pulse</div>
      <button class="btn" onclick="vibratePulse()">Buzz (200ms)</button>
    </div>

    <div class="card card-full">
      <div class="stat-label">Battery & Power</div>
      <div class="stat-val" id="batteryVal">Checking...</div>
      <div id="batteryDetails" style="font-size: 12px; color: var(--text-muted); margin-top: 4px;"></div>
      <button class="btn" onclick="checkBattery()">Refresh Power</button>
    </div>

    <div class="card card-full">
      <div class="stat-label">Text-to-Speech Synthesizer</div>
      <input type="text" id="ttsInput" value="PermScript Studio running native Android APIs!">
      <button class="btn btn-pink" onclick="speakText()">Speak with Android TTS</button>
    </div>

    <div class="card card-full">
      <div class="stat-label">Device Info</div>
      <div id="deviceSpecs" style="font-size: 12px; font-family: monospace; color: var(--text-muted); line-height: 1.5; margin-top: 6px;"></div>
    </div>
  </div>

  <script>
    let torchState = false;

    function toggleTorch() {
      torchState = !torchState;
      if (window.Android) {
        Android.toggleFlashlight(torchState);
        Android.vibrate(60);
      }
      document.getElementById('torchStatus').textContent = torchState ? 'ON' : 'OFF';
      document.getElementById('torchStatus').style.color = torchState ? '#34d399' : '#38bdf8';
    }

    function vibratePulse() {
      if (window.Android) {
        Android.vibrate(200);
        Android.toast("Buzz triggered!");
      }
    }

    function checkBattery() {
      if (window.Android) {
        try {
          const raw = Android.getBatteryInfo();
          const info = JSON.parse(raw);
          document.getElementById('batteryVal').textContent = info.level + '% ' + (info.isCharging ? '⚡ Charging' : 'Discharging');
          document.getElementById('batteryDetails').textContent = 'Temperature: ' + info.temperatureCelsius + '°C';
        } catch(e) {
          document.getElementById('batteryVal').textContent = 'Err: ' + e;
        }
      }
    }

    function speakText() {
      const text = document.getElementById('ttsInput').value;
      if (window.Android) {
        Android.speak(text);
        Android.vibrate(40);
      }
    }

    function loadDeviceInfo() {
      if (window.Android) {
        try {
          const d = JSON.parse(Android.getDeviceInfo());
          document.getElementById('deviceSpecs').innerHTML =
            'Model: ' + d.brand + ' ' + d.model + '<br>' +
            'Android OS: ' + d.androidVersion + ' (SDK ' + d.sdkInt + ')<br>' +
            'Display Size: ' + Math.round(d.screenWidthDp) + 'x' + Math.round(d.screenHeightDp) + ' dp';
        } catch(e) {}
      }
    }

    checkBattery();
    loadDeviceInfo();
  </script>
</body>
</html>"""
            ),

            ScriptProject(
                id = "gps_navigator",
                name = "Live GPS & Compass Navigator",
                description = "High-accuracy geolocation coordinates, speed, altitude, and live animated compass rose",
                iconName = "location",
                requiredPermissions = listOf("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION"),
                htmlCode = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <style>
    :root {
      --bg: #080c14; --surface: #101726; --border: #1e293b;
      --accent: #06b6d4; --text: #f8fafc; --muted: #94a3b8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, sans-serif;
      background: var(--bg); color: var(--text); padding: 16px; text-align: center;
    }
    h1 { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
    p.sub { font-size: 12px; color: var(--muted); margin-bottom: 16px; }
    #compassBox {
      width: 180px; height: 180px; margin: 0 auto 16px;
      border-radius: 50%; border: 3px solid var(--border);
      position: relative; display: flex; align-items: center; justify-content: center;
      box-shadow: 0 0 30px rgba(6,182,212,0.15); background: radial-gradient(#1e293b, #080c14);
    }
    #needle {
      width: 6px; height: 130px; position: absolute;
      background: linear-gradient(to bottom, #ef4444 50%, #f8fafc 50%);
      border-radius: 3px; transform: rotate(0deg); transition: transform 0.1s linear;
    }
    .north-mark {
      position: absolute; top: 6px; font-weight: 900; font-size: 14px; color: #ef4444;
    }
    .stats {
      display: grid; grid-template-columns: 1fr 1fr; gap: 10px; text-align: left;
    }
    .card {
      background: var(--surface); border: 1px solid var(--border);
      border-radius: 10px; padding: 12px;
    }
    .lbl { font-size: 11px; color: var(--muted); text-transform: uppercase; }
    .val { font-size: 15px; font-weight: 700; color: var(--accent); margin-top: 4px; font-family: monospace; }
    .btn {
      width: 100%; border: none; border-radius: 8px; padding: 12px;
      background: var(--accent); color: #000; font-weight: 700; margin-top: 14px;
      cursor: pointer;
    }
  </style>
</head>
<body>
  <h1>GPS & Heading HUD</h1>
  <p class="sub">Precise GPS coordinates and real-time magnetic compass</p>

  <div id="compassBox">
    <div class="north-mark">N</div>
    <div id="needle"></div>
  </div>
  <div style="font-size: 20px; font-weight: 800; margin-bottom: 14px; font-family: monospace;" id="azimuthText">0° N</div>

  <div class="stats">
    <div class="card">
      <div class="lbl">Latitude</div>
      <div class="val" id="latVal">Waiting...</div>
    </div>
    <div class="card">
      <div class="lbl">Longitude</div>
      <div class="val" id="lngVal">Waiting...</div>
    </div>
    <div class="card">
      <div class="lbl">Accuracy</div>
      <div class="val" id="accVal">--</div>
    </div>
    <div class="card">
      <div class="lbl">Speed</div>
      <div class="val" id="speedVal">--</div>
    </div>
  </div>

  <button class="btn" onclick="requestGPS()">Update GPS Position</button>

  <script>
    function requestGPS() {
      if (window.Android) {
        Android.getLocation();
        Android.vibrate(50);
        Android.toast("Fetching GPS fix...");
      }
    }

    // Called automatically by native bridge
    window.onLocationResult = function(lat, lng, acc, alt, speed) {
      document.getElementById('latVal').textContent = lat.toFixed(5);
      document.getElementById('lngVal').textContent = lng.toFixed(5);
      document.getElementById('accVal').textContent = '±' + Math.round(acc) + ' m';
      document.getElementById('speedVal').textContent = (speed * 3.6).toFixed(1) + ' km/h';
      if (window.Android) Android.vibrate(80);
    };

    // Called automatically by native sensor helper
    window.onSensorData = function(type, x, y, z) {
      if (type === 'compass') {
        const deg = Math.round(x);
        document.getElementById('needle').style.transform = 'rotate(' + (-deg) + 'deg)';
        const dir = deg > 337 || deg < 23 ? 'N' : deg < 68 ? 'NE' : deg < 113 ? 'E' : deg < 158 ? 'SE' : deg < 203 ? 'S' : deg < 248 ? 'SW' : deg < 293 ? 'W' : 'NW';
        document.getElementById('azimuthText').textContent = deg + '° ' + dir;
      }
    };

    if (window.Android) {
      Android.startSensorStream();
      requestGPS();
    }
  </script>
</body>
</html>"""
            ),

            ScriptProject(
                id = "sound_meter",
                name = "Live Sound Meter & Decibel Visualizer",
                description = "Real-time microphone amplitude waveform, decibel meter, and noise level indicator",
                iconName = "mic",
                requiredPermissions = listOf("RECORD_AUDIO"),
                htmlCode = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <style>
    :root {
      --bg: #09090b; --card: #18181b; --border: #27272a;
      --accent: #22c55e; --red: #ef4444; --text: #fafafa; --muted: #a1a1aa;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, sans-serif;
      background: var(--bg); color: var(--text); padding: 18px; text-align: center;
    }
    h1 { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
    p.sub { font-size: 13px; color: var(--muted); margin-bottom: 20px; }
    .meter-circle {
      width: 170px; height: 170px; border-radius: 50%;
      border: 6px solid var(--border); margin: 0 auto 18px;
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      position: relative; background: #121215;
    }
    .db-val { font-size: 42px; font-weight: 800; font-family: monospace; color: var(--accent); }
    .db-unit { font-size: 12px; color: var(--muted); text-transform: uppercase; font-weight: 600; }
    .bar-container {
      width: 100%; height: 22px; background: #121215; border-radius: 12px;
      overflow: hidden; border: 1px solid var(--border); margin-bottom: 14px;
    }
    #meterFill {
      height: 100%; width: 10%; background: linear-gradient(to right, #22c55e, #eab308, #ef4444);
      transition: width 0.08s ease;
    }
    .tags {
      display: flex; justify-content: space-between; font-size: 11px; color: var(--muted); margin-bottom: 20px;
    }
    .btn {
      width: 100%; padding: 12px; border-radius: 10px; border: none;
      font-size: 14px; font-weight: 700; cursor: pointer;
      background: var(--accent); color: #000;
    }
    .info-card {
      background: var(--card); border: 1px solid var(--border);
      border-radius: 12px; padding: 14px; margin-top: 18px; text-align: left;
    }
    .info-row { display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13px; }
  </style>
</head>
<body>
  <h1>Microphone Audio Meter</h1>
  <p class="sub">Real-time RMS acoustic measurement via native AudioRecord</p>

  <div class="meter-circle">
    <div class="db-val" id="dbVal">0</div>
    <div class="db-unit">Decibels (dB)</div>
  </div>

  <div class="bar-container">
    <div id="meterFill"></div>
  </div>
  <div class="tags">
    <span>Quiet (30dB)</span>
    <span>Normal (60dB)</span>
    <span>Loud (85dB+)</span>
  </div>

  <button class="btn" id="toggleBtn" onclick="toggleRecording()">Start Audio Stream</button>

  <div class="info-card">
    <div class="info-row"><span style="color:var(--muted)">Max Peak dB:</span><strong id="peakVal">--</strong></div>
    <div class="info-row"><span style="color:var(--muted)">Peak Amplitude:</span><strong id="ampVal">--</strong></div>
    <div class="info-row"><span style="color:var(--muted)">Environment:</span><strong id="envStatus">Silent</strong></div>
  </div>

  <script>
    let isRunning = false;
    let maxDb = 0;

    function toggleRecording() {
      if (!window.Android) return;
      if (!isRunning) {
        Android.startSoundMeter();
        document.getElementById('toggleBtn').textContent = 'Stop Audio Stream';
        document.getElementById('toggleBtn').style.background = '#ef4444';
        isRunning = true;
      } else {
        Android.stopSoundMeter();
        document.getElementById('toggleBtn').textContent = 'Start Audio Stream';
        document.getElementById('toggleBtn').style.background = '#22c55e';
        isRunning = false;
      }
    }

    window.onSoundMeter = function(db, amp) {
      const roundedDb = Math.round(db);
      document.getElementById('dbVal').textContent = roundedDb;
      const pct = Math.min(100, Math.max(0, (db / 110) * 100));
      document.getElementById('meterFill').style.width = pct + '%';

      if (db > maxDb) {
        maxDb = db;
        document.getElementById('peakVal').textContent = Math.round(maxDb) + ' dB';
      }
      document.getElementById('ampVal').textContent = Math.round(amp);

      const env = db < 45 ? 'Quiet Whisper' : db < 70 ? 'Conversational Speech' : db < 85 ? 'Loud Noise' : '🚨 Hazardous (85dB+)';
      document.getElementById('envStatus').textContent = env;
    };
  </script>
</body>
</html>"""
            ),

            ScriptProject(
                id = "notification_lab",
                name = "Native Notification Dispatcher",
                description = "Dispatch high-priority Android notifications with custom titles, messages, and vibration directly from JavaScript",
                iconName = "notifications",
                requiredPermissions = listOf("POST_NOTIFICATIONS", "VIBRATE"),
                htmlCode = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <style>
    :root {
      --bg: #0d1117; --card: #161b22; --border: #30363d;
      --accent: #a855f7; --text: #f0f6fc; --muted: #8b949e;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, sans-serif;
      background: var(--bg); color: var(--text); padding: 18px;
    }
    h1 { font-size: 20px; font-weight: 700; margin-bottom: 6px; }
    p.sub { font-size: 13px; color: var(--muted); margin-bottom: 20px; }
    .form-group { margin-bottom: 14px; text-align: left; }
    label { display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; color: var(--muted); margin-bottom: 6px; }
    input[type="text"], textarea {
      width: 100%; background: #0b0f14; border: 1px solid var(--border);
      color: var(--text); padding: 10px 12px; border-radius: 8px; font-size: 14px;
    }
    textarea { height: 75px; resize: none; }
    .btn {
      width: 100%; padding: 12px; border-radius: 8px; border: none;
      font-size: 14px; font-weight: 700; cursor: pointer;
      background: var(--accent); color: #fff; margin-top: 8px;
    }
    .btn-timer {
      background: #238636; margin-top: 10px;
    }
    .log-box {
      margin-top: 20px; background: #0b0f14; border: 1px solid var(--border);
      border-radius: 8px; padding: 12px; font-family: monospace; font-size: 12px;
      color: var(--muted); min-height: 80px; text-align: left;
    }
  </style>
</head>
<body>
  <h1>Notification Dispatcher</h1>
  <p class="sub">Post system-tray alerts directly from your code</p>

  <div class="form-group">
    <label>Notification Title</label>
    <input type="text" id="notifTitle" value="🚀 Rocket Launched!">
  </div>

  <div class="form-group">
    <label>Notification Message</label>
    <textarea id="notifMsg">Your scheduled background task completed successfully without errors.</textarea>
  </div>

  <button class="btn" onclick="sendNow()">Send Notification Now</button>
  <button class="btn btn-timer" onclick="sendDelayed(5)">Send with 5-Second Delay</button>

  <div class="log-box" id="logArea">Event Log:<br></div>

  <script>
    function logEvent(msg) {
      document.getElementById('logArea').innerHTML += '&gt; ' + msg + '<br>';
    }

    function sendNow() {
      const title = document.getElementById('notifTitle').value;
      const msg = document.getElementById('notifMsg').value;
      if (window.Android) {
        Android.notify(title, msg);
        Android.vibrate(100);
        logEvent('Sent notification: ' + title);
      } else {
        logEvent('Android API bridge not available');
      }
    }

    function sendDelayed(seconds) {
      logEvent('Queued notification in ' + seconds + 's...');
      setTimeout(() => {
        sendNow();
      }, seconds * 1000);
    }
  </script>
</body>
</html>"""
            ),

            ScriptProject(
                id = "accelerometer_physics",
                name = "Motion Physics & Shake Detector",
                description = "Interactive tilt physics canvas driven by accelerometer and gyro data + shake vibration alert",
                iconName = "sensor",
                requiredPermissions = listOf("BODY_SENSORS", "ACTIVITY_RECOGNITION", "VIBRATE"),
                htmlCode = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <style>
    :root {
      --bg: #0a0a0f; --surface: #14141e; --border: #232336;
      --accent: #f59e0b; --text: #f8fafc; --muted: #94a3b8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, sans-serif;
      background: var(--bg); color: var(--text); padding: 14px; text-align: center;
      touch-action: none;
    }
    h1 { font-size: 18px; font-weight: 700; margin-bottom: 2px; }
    p.sub { font-size: 11px; color: var(--muted); margin-bottom: 12px; }
    #canvas {
      background: #000; border: 2px solid var(--border); border-radius: 14px;
      display: block; margin: 0 auto 12px; width: 100%; max-width: 340px; height: 260px;
    }
    .hud {
      display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; font-family: monospace; font-size: 12px;
    }
    .hud-box {
      background: var(--surface); border: 1px solid var(--border); border-radius: 8px; padding: 6px;
    }
    .hud-lbl { color: var(--muted); font-size: 10px; }
  </style>
</head>
<body>
  <h1>Tilt Physics Simulator</h1>
  <p class="sub">Tilt your device to roll the neon orb using Accelerometer</p>

  <canvas id="canvas" width="340" height="260"></canvas>

  <div class="hud">
    <div class="hud-box"><div class="hud-lbl">ACCEL X</div><div id="accX">0.0</div></div>
    <div class="hud-box"><div class="hud-lbl">ACCEL Y</div><div id="accY">0.0</div></div>
    <div class="hud-box"><div class="hud-lbl">ACCEL Z</div><div id="accZ">0.0</div></div>
  </div>

  <script>
    const canvas = document.getElementById('canvas');
    const ctx = canvas.getContext('2d');
    let x = canvas.width / 2;
    let y = canvas.height / 2;
    let vx = 0;
    let vy = 0;
    const radius = 18;

    let targetAx = 0;
    let targetAy = 0;
    let lastShakeTime = 0;

    window.onSensorData = function(type, ax, ay, az) {
      if (type === 'accelerometer') {
        // Invert X for phone tilt orientation
        targetAx = -ax * 0.4;
        targetAy = ay * 0.4;

        document.getElementById('accX').textContent = ax.toFixed(1);
        document.getElementById('accY').textContent = ay.toFixed(1);
        document.getElementById('accZ').textContent = az.toFixed(1);

        // Shake detection
        const speed = Math.abs(ax) + Math.abs(ay) + Math.abs(az);
        if (speed > 25 && Date.now() - lastShakeTime > 1000) {
          lastShakeTime = Date.now();
          if (window.Android) {
            Android.vibrate(150);
            Android.toast("⚡ Shake gesture detected!");
          }
        }
      }
    };

    function update() {
      vx += targetAx;
      vy += targetAy;
      vx *= 0.94; // friction
      vy *= 0.94;

      x += vx;
      y += vy;

      // Bounce
      if (x - radius < 0) { x = radius; vx = -vx * 0.7; if (window.Android && Math.abs(vx) > 3) Android.vibrate(25); }
      if (x + radius > canvas.width) { x = canvas.width - radius; vx = -vx * 0.7; if (window.Android && Math.abs(vx) > 3) Android.vibrate(25); }
      if (y - radius < 0) { y = radius; vy = -vy * 0.7; if (window.Android && Math.abs(vy) > 3) Android.vibrate(25); }
      if (y + radius > canvas.height) { y = canvas.height - radius; vy = -vy * 0.7; if (window.Android && Math.abs(vy) > 3) Android.vibrate(25); }

      // Draw
      ctx.fillStyle = '#080811';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Grid
      ctx.strokeStyle = '#181829';
      ctx.lineWidth = 1;
      for (let i = 0; i < canvas.width; i += 30) { ctx.beginPath(); ctx.moveTo(i, 0); ctx.lineTo(i, canvas.height); ctx.stroke(); }
      for (let j = 0; j < canvas.height; j += 30) { ctx.beginPath(); ctx.moveTo(0, j); ctx.lineTo(canvas.width, j); ctx.stroke(); }

      // Orb glow
      const grad = ctx.createRadialGradient(x, y, 2, x, y, radius * 1.5);
      grad.addColorStop(0, '#f59e0b');
      grad.addColorStop(1, 'rgba(245, 158, 11, 0)');
      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.arc(x, y, radius * 1.5, 0, Math.PI * 2);
      ctx.fill();

      // Ball
      ctx.fillStyle = '#fbbf24';
      ctx.beginPath();
      ctx.arc(x, y, radius, 0, Math.PI * 2);
      ctx.fill();

      requestAnimationFrame(update);
    }

    if (window.Android) {
      Android.startSensorStream();
    }
    update();
  </script>
</body>
</html>"""
            ),

            ScriptProject(
                id = "contacts_calendar",
                name = "Contacts & Calendar Inspector",
                description = "Query and inspect address book contacts and calendar schedules with native permission checks",
                iconName = "contacts",
                requiredPermissions = listOf("READ_CONTACTS", "READ_CALENDAR"),
                htmlCode = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <style>
    :root {
      --bg: #0b0f17; --card: #151d2c; --border: #223048;
      --accent: #38bdf8; --text: #f1f5f9; --muted: #94a3b8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, sans-serif;
      background: var(--bg); color: var(--text); padding: 16px;
    }
    h1 { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
    p.sub { font-size: 13px; color: var(--muted); margin-bottom: 16px; }
    .tabs { display: flex; gap: 8px; margin-bottom: 14px; }
    .tab {
      flex: 1; padding: 10px; border-radius: 8px; border: 1px solid var(--border);
      background: var(--card); color: var(--muted); font-weight: 600; font-size: 13px;
      cursor: pointer; text-align: center;
    }
    .tab.active { background: var(--accent); color: #000; border-color: var(--accent); }
    .list { display: flex; flex-direction: column; gap: 8px; max-height: 380px; overflow-y: auto; }
    .item {
      background: var(--card); border: 1px solid var(--border); border-radius: 8px;
      padding: 10px 14px; display: flex; flex-direction: column; text-align: left;
    }
    .item-title { font-weight: 600; font-size: 14px; }
    .item-sub { font-size: 12px; color: var(--muted); margin-top: 3px; font-family: monospace; }
    .btn {
      width: 100%; padding: 12px; border-radius: 8px; border: none;
      background: var(--accent); color: #000; font-weight: 700; margin-top: 14px;
      cursor: pointer;
    }
  </style>
</head>
<body>
  <h1>Contacts & Calendar</h1>
  <p class="sub">Direct read-only inspection of device records</p>

  <div class="tabs">
    <div class="tab active" id="tabContacts" onclick="setTab('contacts')">Contacts</div>
    <div class="tab" id="tabCalendar" onclick="setTab('calendar')">Calendar</div>
  </div>

  <div class="list" id="contentList">
    <div style="color:var(--muted); font-size:13px; padding:20px; text-align:center;">
      Tap "Load Data" below to query Android records.
    </div>
  </div>

  <button class="btn" onclick="loadData()">Load Data from Device</button>

  <script>
    let activeTab = 'contacts';

    function setTab(tab) {
      activeTab = tab;
      document.getElementById('tabContacts').className = 'tab ' + (tab === 'contacts' ? 'active' : '');
      document.getElementById('tabCalendar').className = 'tab ' + (tab === 'calendar' ? 'active' : '');
      loadData();
    }

    function loadData() {
      const container = document.getElementById('contentList');
      container.innerHTML = '<div style="color:var(--muted); padding:20px; text-align:center;">Loading...</div>';

      if (!window.Android) {
        container.innerHTML = '<div style="color:#ef4444; padding:20px;">Android native bridge not found.</div>';
        return;
      }

      if (activeTab === 'contacts') {
        try {
          const raw = Android.getContacts(20);
          const list = JSON.parse(raw);
          if (list.length === 0) {
            container.innerHTML = '<div style="color:var(--muted); padding:20px; text-align:center;">No contacts found (check permission).</div>';
            return;
          }
          container.innerHTML = list.map(c =>
            '<div class="item"><span class="item-title">' + (c.name || 'Unnamed') + '</span><span class="item-sub">' + (c.phone || 'No phone') + '</span></div>'
          ).join('');
        } catch(e) {
          container.innerHTML = '<div style="color:#ef4444; padding:20px;">Error: ' + e + '</div>';
        }
      } else {
        try {
          const raw = Android.getCalendarEvents(20);
          const list = JSON.parse(raw);
          if (list.length === 0) {
            container.innerHTML = '<div style="color:var(--muted); padding:20px; text-align:center;">No calendar events found.</div>';
            return;
          }
          container.innerHTML = list.map(ev => {
            const dateStr = ev.startTime ? new Date(ev.startTime).toLocaleString() : 'Date N/A';
            return '<div class="item"><span class="item-title">' + (ev.title || 'Event') + '</span><span class="item-sub">' + dateStr + (ev.location ? ' • ' + ev.location : '') + '</span></div>';
          }).join('');
        } catch(e) {
          container.innerHTML = '<div style="color:#ef4444; padding:20px;">Error: ' + e + '</div>';
        }
      }
    }
  </script>
</body>
</html>"""
            ),

            ScriptProject(
                id = "blank_starter",
                name = "Blank Starter Template",
                description = "Clean minimal starter template with full Android.* JS API bindings",
                iconName = "code",
                requiredPermissions = emptyList(),
                htmlCode = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <title>My PermScript App</title>
  <style>
    body {
      background: #0d1117; color: #f0f6fc;
      font-family: system-ui, -apple-system, sans-serif;
      padding: 20px; text-align: center;
    }
    h1 { color: #38bdf8; font-size: 24px; margin-bottom: 12px; }
    p { color: #8b949e; font-size: 14px; margin-bottom: 24px; }
    button {
      background: #38bdf8; color: #000; border: none;
      padding: 12px 24px; border-radius: 8px; font-weight: 700;
      font-size: 14px; cursor: pointer;
    }
  </style>
</head>
<body>
  <h1>Hello from PermScript!</h1>
  <p>Edit this code in the Studio tab and tap RUN to execute instantly.</p>
  <button onclick="onTap()">Trigger Native Action</button>

  <script>
    function onTap() {
      if (window.Android) {
        Android.toast("Hello from in-app JavaScript!");
        Android.vibrate(100);
        console.log("Button clicked!");
      } else {
        alert("Running outside Android bridge");
      }
    }
  </script>
</body>
</html>"""
            )
        )
    }
}
