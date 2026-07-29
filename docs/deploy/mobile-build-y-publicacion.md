# Build firmado y publicación en stores (mobile)

La app mobile (`mobile/`, `com.easybeach.mobile`) es una sola app blanco-etiquetada
(ADR-005: theming en runtime por balneario) - **no hace falta publicar una app
por balneario**, un solo build de Android y uno de iOS sirven para todos los
tenants.

Este documento describe el proceso real de firma y publicación. A diferencia
de `backend-ci.yml`/`web-ci.yml`/`deploy.yml`, este proceso **no está
automatizado en CI todavía**, porque requiere dos cuentas de desarrollador
reales que sólo un humano puede crear (identidad real + medio de pago real):

- **Google Play Console**: cuenta de desarrollador, pago único de USD 25.
- **Apple Developer Program**: cuenta de desarrollador, USD 99/año.

Ninguna de las dos es algo que se pueda crear sin decisiones humanas de
titularidad y presupuesto - por eso quedan fuera del alcance de lo que este
proyecto puede automatizar hoy.

## Android

1. **Generar el keystore de firma** (una sola vez, guardar fuera del repo):
   ```bash
   keytool -genkeypair -v -storetype PKCS12 \
     -keystore easybeach-release.keystore \
     -alias easybeach -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Configurar `mobile/android/gradle.properties` (no versionado) con
   `MYAPP_RELEASE_STORE_FILE`, `MYAPP_RELEASE_KEY_ALIAS` y las contraseñas, y
   el `signingConfigs.release` correspondiente en
   `mobile/android/app/build.gradle` (estándar de React Native, ver su
   documentación de "Signed APK").
3. Build del bundle de release:
   ```bash
   cd mobile/android && ./gradlew bundleRelease
   # Salida: android/app/build/outputs/bundle/release/app-release.aab
   ```
4. En Google Play Console: crear la app, completar ficha de la store
   (descripción, capturas, política de privacidad, cuestionario de
   clasificación de contenido), subir el `.aab` primero a un track interno de
   testing, y recién después promoverlo a producción.

## iOS

1. En Apple Developer Program: crear el App ID (`com.easybeach.mobile`), el
   certificado de distribución y el provisioning profile de App Store.
2. `cd mobile/ios && pod install`.
3. Abrir `mobile/ios/*.xcworkspace` en Xcode, seleccionar el scheme de
   Release, y generar el archive (Product > Archive).
4. Subir el archive a App Store Connect (Xcode Organizer o Transporter).
5. Completar la ficha en App Store Connect y enviar a review de Apple.
6. Usar TestFlight para testing interno antes de la publicación pública.

## Automatizar esto a futuro

Una vez que las dos cuentas y credenciales de firma existan, el paso natural
es automatizar los builds (no la creación de cuentas ni el primer submit, que
siempre requieren intervención humana) con **fastlane** en un
`mobile-release.yml` similar a `deploy.yml`: guardar el keystore/certificados
como secrets en base64, y correr `fastlane android/ios` en un workflow
disparado manualmente. No se escribió ese workflow ahora porque no hay
credenciales reales contra las que probarlo - un workflow sin verificar no es
mejor que este documento.
