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

  CreateShortCut "$DESKTOP\Capsule Player.lnk" "$INSTDIR\Capsule.exe"
  CreateShortCut "$DESKTOP\Capsule Studio.lnk" "$INSTDIR\Capsule.exe -studio"
  CreateDirectory "$SMPROGRAMS\Capsule"
  CreateShortCut "$SMPROGRAMS\Capsule\Capsule Player.lnk" "$INSTDIR\Capsule.exe"
  CreateShortCut "$SMPROGRAMS\Capsule\Capsule Studio.lnk" "$INSTDIR\Capsule.exe"


  ; --------------------
  ; Programı çalıştır
  ; --------------------
  Exec '"$INSTDIR\Capsule.exe"'

SectionEnd
