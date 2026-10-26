# Inicio — launcher limpio para Mi TV Stick (MiTV-AESP0)

Pantalla de inicio minimalista: reloj grande, fecha, y solo las apps que tú elijas.
Sin recomendaciones, sin publicidad, sin servicios en segundo plano y sin librerías
externas (pesa muy poco, pensado para el 1 GB de RAM del Stick).

## Cómo se usa en la TV

- **OK**: abre la app.
- **Mantener OK presionado** sobre una app de Inicio: mover a la izquierda/derecha,
  mover al principio, quitar de Inicio o ver información.
- **Todas las apps**: muestra todo lo instalado. Mantén OK para agregar o quitar de Inicio.
- **Descargar apps**: abre Google Play (ver sección "Cómo descargar apps").
- **Personalizar**: tema de color, fondo con tu propia imagen, clima de Iquique y fila de recientes.
- **Ajustes**: abre la configuración normal de Android TV.
- **Recientes**: las últimas 4 apps que abriste desde Inicio aparecen arriba.
- La primera vez, Inicio muestra todas las apps de TV; quita las que no uses.

---

## Paso 1 — Obtener el APK (compilado gratis en GitHub)

1. Crea una cuenta en https://github.com si no tienes.
2. Crea un repositorio nuevo (por ejemplo `InicioTV`), puede ser privado.
3. Usa **"uploading an existing file"** y arrastra **todo el contenido** de esta carpeta
   (incluida la carpeta `.github`; si el navegador la oculta, súbela aparte con la misma ruta
   `.github/workflows/build.yml`).
4. Ve a la pestaña **Actions**. Se ejecuta "Compilar APK" (unos 3–5 minutos).
5. Al terminar en verde, abre la ejecución y descarga **InicioTV-apk** (viene en un .zip).
   Descomprime y tendrás `InicioTV.apk`.

> Alternativa: abrir la carpeta en Android Studio y usar *Build → Build APK(s)*.

## Paso 2 — Activar ADB en el Stick

1. **Ajustes → Preferencias del dispositivo → Información**.
2. Presiona OK **7 veces** sobre **Compilación** hasta que diga que eres desarrollador.
3. Vuelve a **Preferencias del dispositivo → Opciones para desarrolladores** y activa
   **Depuración USB / ADB**.
4. Anota la IP del Stick en **Ajustes → Red e Internet → (tu Wi-Fi)**.

En el computador (misma red Wi-Fi) descarga *Android SDK Platform-Tools*:
https://developer.android.com/tools/releases/platform-tools
Descomprime, abre una terminal en esa carpeta y conecta:

```
adb connect 192.168.X.X:5555
```

En la TV aparece un aviso: elige **Permitir siempre**.

## Paso 3 — Instalar y dejarlo como pantalla de inicio

```
adb install -r InicioTV.apk
adb shell cmd package set-home-activity cl.jc.inicio/.MainActivity
```

Presiona **Home** en el control. Si sigue apareciendo la pantalla de Google, desactiva
el launcher original (no se borra, se puede revertir):

```
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

Opcional, quita el servicio de recomendaciones (ahorra RAM):

```
adb shell pm disable-user --user 0 com.google.android.tvrecommendations
```

## Personalizar

Desde la tarjeta **Personalizar** (usa ◀ ▶ u OK para cambiar):

- **Tema de color**: Medianoche, Océano, Desierto, Atardecer o Grafito.
- **Fondo**: degradado del tema o **imagen propia**. Para la imagen, guarda una foto
  llamada `fondo.jpg` (ideal 1920×1080, horizontal) en la carpeta **Download** del Stick.
  La forma fácil es la app **Send Files to TV**, instalada en el celular y en la TV.
  La primera vez, Inicio pide permiso para leer archivos: acepta. Con ADB también sirve:
  `adb push fondo.jpg /sdcard/Download/fondo.jpg`
- **Clima en Iquique**: temperatura y estado actual (fuente: Open-Meteo, gratis y sin
  cuenta). Se actualiza cada 30 minutos. Para otra ciudad, cambia `LAT`, `LON` y `CITY`
  en `Weather.java`.
- **Fila de recientes**: mostrar u ocultar.

> Sobre "seguir viendo": la fila con el episodio exacto donde quedaste solo la puede leer
> el launcher de Google, porque Android reserva ese permiso para apps del sistema.
> Por eso Inicio usa **Recientes**, que vuelve a la app y ella retoma donde quedaste.

## Cómo descargar apps

1. **Desde la TV**: tarjeta **Descargar apps** → Google Play → busca con el micrófono del
   control o escribiendo. Instala y la app aparece en **Todas las apps** (mantén OK para
   agregarla a Inicio).
2. **Desde el celular**: en la app de Google Play, busca la app, toca la flecha junto a
   **Instalar** y marca el Mi TV Stick. Se instala sola en la TV.
3. **Apps que no están en Google Play** (APK): instala **Downloader** desde Google Play,
   dale permiso en *Ajustes → Apps → Seguridad → Fuentes desconocidas* y escribe la
   dirección del APK. Descarga solo desde el sitio oficial de cada app.

## Volver atrás (deshacer todo)

```
adb shell pm enable com.google.android.tvlauncher
adb shell pm enable com.google.android.tvrecommendations
adb uninstall cl.jc.inicio
```

## Importante

- **Instala y prueba Inicio ANTES de desactivar el launcher original.** Si desactivas el
  original sin tener otro, el Stick queda sin pantalla de inicio (se arregla con
  `pm enable`, pero necesitas ADB).
- No desactives otros paquetes al azar. Si quieres limpiar más, saca la lista con
  `adb shell pm list packages` y revísala antes.
