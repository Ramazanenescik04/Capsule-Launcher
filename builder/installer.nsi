; --------------------
; Genel ayarlar
; --------------------
SilentInstall silent
RequestExecutionLevel user
OutFile "CapsuleSetup.exe"
InstallDir "$APPDATA\.capsule"

; --------------------
Section "Install"

  ; klasörü oluştur
  SetOutPath "$INSTDIR"

  ; --------------------
  ; Dosyaları kopyala
  ; --------------------

  ; launcher jar
  File "Capsule.exe"

  ; runtime (jlink çıktısı)
  File /r "runtime"

  ; iconlar
  File "player.ico"
  File "studio.ico"

  CreateShortCut "$DESKTOP\Capsule Player.lnk" "$INSTDIR\Capsule.exe" "" "$INSTDIR\player.ico"
  CreateShortCut "$DESKTOP\Capsule Studio.lnk" "$INSTDIR\Capsule.exe" "-studio" "$INSTDIR\studio.ico"
  CreateDirectory "$SMPROGRAMS\Capsule"
  CreateShortCut "$SMPROGRAMS\Capsule\Capsule Player.lnk" "$INSTDIR\Capsule.exe" "" "$INSTDIR\player.ico"
  CreateShortCut "$SMPROGRAMS\Capsule\Capsule Studio.lnk" "$INSTDIR\Capsule.exe" "-studio" "$INSTDIR\studio.ico"


  ; --------------------
  ; Programı çalıştır
  ; --------------------
  Exec '"$INSTDIR\Capsule.exe"'

SectionEnd
