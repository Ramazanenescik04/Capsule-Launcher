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

  ; capsule:// protocol
  WriteRegStr HKCU "Software\Classes\capsule" "" "URL:Capsule Protocol"
  WriteRegStr HKCU "Software\Classes\capsule" "URL Protocol" ""

  WriteRegStr HKCU "Software\Classes\capsule\DefaultIcon" "" "$INSTDIR\Capsule.exe,0"

  WriteRegStr HKCU "Software\Classes\capsule\shell\open\command" "" \
  '"$INSTDIR\Capsule.exe" "%1"'

  WriteUninstaller "$INSTDIR\Uninstall.exe"

  CreateShortCut "$DESKTOP\Capsule Player.lnk" "$INSTDIR\Capsule.exe" "" "$INSTDIR\player.ico"
  CreateShortCut "$DESKTOP\Capsule Studio.lnk" "$INSTDIR\Capsule.exe" "-studio" "$INSTDIR\studio.ico"
  CreateDirectory "$SMPROGRAMS\Capsule"
  CreateShortCut "$SMPROGRAMS\Capsule\Capsule Player.lnk" "$INSTDIR\Capsule.exe" "" "$INSTDIR\player.ico"
  CreateShortCut "$SMPROGRAMS\Capsule\Capsule Studio.lnk" "$INSTDIR\Capsule.exe" "-studio" "$INSTDIR\studio.ico"

  ; ===== Add/Remove Programs kaydı =====
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Capsule" \
    "DisplayName" "Capsule"

  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Capsule" \
    "UninstallString" '"$INSTDIR\Uninstall.exe"'

  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Capsule" \
    "InstallLocation" "$INSTDIR"

  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Capsule" \
    "DisplayIcon" "$INSTDIR\player.ico"

  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Capsule" \
  "Publisher" "EmirE"

  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Capsule" \
    "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Capsule" \
    "NoRepair" 1

  ; --------------------
  ; Programı çalıştır
  ; --------------------
  Exec '"$INSTDIR\Capsule.exe"'
  Quit
SectionEnd

Section "Uninstall"

  ; ===== Custom protocol (capsule://) =====
  DeleteRegKey HKCU "Software\Classes\capsule"
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Capsule"

  ; ===== Masaüstü kısayolu =====
  Delete "$DESKTOP\Capsule Player.lnk"
  Delete "$DESKTOP\Capsule Studio.lnk"

  ; ===== Start Menu kısayolu (varsa) =====
  Delete "$SMPROGRAMS\Capsule\Capsule Player.lnk"
  Delete "$SMPROGRAMS\Capsule\Capsule Studio.lnk"
  RMDir  "$SMPROGRAMS\Capsule"

  ; ===== Kurulum dizini =====
  RMDir /r "$INSTDIR"

SectionEnd


