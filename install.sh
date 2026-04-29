#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

APP_NAME="system-services"
APP_DIR="$HOME/.local/share/applications/edfcsx/$APP_NAME"
DESKTOP_DIR="$HOME/.local/share/applications"
ICON_DIR="$HOME/.local/share/icons/hicolor/256x256/apps"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log()  { echo -e "${GREEN}[✔]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1" >&2; exit 1; }

# ── Verifica/instala Java 17+ ────────────────────────────────────────────────

install_java() {
    if command -v java &>/dev/null; then
        JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "${JAVA_VER:-0}" -ge 17 ] 2>/dev/null; then
            log "Java $JAVA_VER detectado."
            return
        fi
        warn "Java detectado mas versão < 17. Instalando Java 17..."
    else
        warn "Java não encontrado. Instalando..."
    fi

    if command -v apt-get &>/dev/null; then
        sudo apt-get update -qq
        sudo apt-get install -y default-jre
    elif command -v dnf &>/dev/null; then
        sudo dnf install -y java-17-openjdk
    elif command -v pacman &>/dev/null; then
        sudo pacman -S --noconfirm jre17-openjdk
    elif command -v zypper &>/dev/null; then
        sudo zypper install -y java-17-openjdk
    else
        err "Gerenciador de pacotes não reconhecido. Instale o Java 17+ manualmente e rode o script novamente."
    fi
}

# ── Localiza os arquivos de distribuição ─────────────────────────────────────

JAR_FILE=$(find "$SCRIPT_DIR" -maxdepth 1 -name "*.jar" | head -1)
[ -z "$JAR_FILE" ] && err "Nenhum arquivo .jar encontrado em $SCRIPT_DIR"

ICON_FILE="$SCRIPT_DIR/icon.png"
[ ! -f "$ICON_FILE" ] && err "Arquivo icon.png não encontrado em $SCRIPT_DIR"

# ── Instalação ───────────────────────────────────────────────────────────────

install_java

mkdir -p "$APP_DIR" "$DESKTOP_DIR" "$ICON_DIR"

cp "$JAR_FILE"   "$APP_DIR/$APP_NAME.jar"
cp "$ICON_FILE"  "$APP_DIR/icon.png"
cp "$ICON_FILE"  "$ICON_DIR/$APP_NAME.png"
log "Arquivos instalados em $APP_DIR"

cat > "$DESKTOP_DIR/$APP_NAME.desktop" << EOF
[Desktop Entry]
Name=System Services
Comment=Monitore e controle serviços e portas do sistema
Exec=java -jar $APP_DIR/$APP_NAME.jar
Icon=$APP_NAME
Terminal=false
Type=Application
Categories=System;
StartupWMClass=$APP_NAME
EOF

chmod +x "$DESKTOP_DIR/$APP_NAME.desktop"
log ".desktop criado em $DESKTOP_DIR/$APP_NAME.desktop"

# ── Atualiza caches ──────────────────────────────────────────────────────────

if command -v update-desktop-database &>/dev/null; then
    update-desktop-database "$DESKTOP_DIR" 2>/dev/null || true
fi

if command -v gtk-update-icon-cache &>/dev/null; then
    gtk-update-icon-cache -f "$HOME/.local/share/icons/hicolor" 2>/dev/null || true
fi

log "Caches atualizados."
echo ""
echo -e "${GREEN}Instalação concluída!${NC} Abra o System Services pelo menu de aplicativos."
