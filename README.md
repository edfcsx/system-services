# System Services

Aplicativo desktop para Linux que permite monitorar e controlar serviços systemd e portas de rede abertas, tudo em uma interface gráfica com tema escuro.

![Java 17](https://img.shields.io/badge/Java-17-blue) ![Swing](https://img.shields.io/badge/UI-Swing-informational) ![Gradle](https://img.shields.io/badge/Build-Gradle-02303A)

## Funcionalidades

### Aba Serviços
- Lista todos os serviços systemd (equivalente a `systemctl list-units --type=service --all`)
- Filtro em tempo real por nome, descrição ou status
- Ações de **iniciar**, **parar** e **reiniciar** com confirmação antes de executar
- Autenticação via polkit (diálogo gráfico) com fallback para `sudo`
- Chips no cabeçalho com contagem de serviços ativos e com falha
- Duplo clique abre painel de detalhes do serviço

### Aba Portas
- Lista todas as portas em escuta TCP e UDP (via `ss -tunlp`)
- Filtro por número de porta, protocolo, endereço ou nome do processo
- **Encerrar processo** vinculado à porta via SIGTERM, com confirmação
- Autenticação via polkit ou `sudo` para processos de outros usuários
- Chips com total de portas e quantidade com processo identificado
- Duplo clique abre painel de detalhes da porta

## Pré-requisitos

| Dependência | Versão mínima | Observação |
|---|---|---|
| Java | 17 | Instalado automaticamente pelo `install.sh` |
| systemd | qualquer | Necessário para a aba Serviços |
| iproute2 (`ss`) | qualquer | Necessário para a aba Portas |
| polkit ou sudo | qualquer | Para ações privilegiadas |

## Instalação

### 1. Compilar o JAR

```bash
./gradlew jar
```

O arquivo gerado fica em `build/libs/system-services-1.0-SNAPSHOT.jar`.

### 2. Instalar no sistema

Copie o JAR e o `icon.png` para um diretório e execute o script de instalação:

```bash
cp build/libs/system-services-1.0-SNAPSHOT.jar ./
cp src/main/resources/icon.png ./
bash install.sh
```

O script realiza as seguintes etapas automaticamente:

- Verifica e instala o Java 17 caso necessário (suporta apt, dnf, pacman e zypper)
- Copia os arquivos para `~/.local/share/applications/edfcsx/system-services/`
- Cria a entrada `.desktop` em `~/.local/share/applications/`
- Registra o ícone em `~/.local/share/icons/hicolor/256x256/apps/`
- Atualiza os caches do desktop e de ícones

Após a instalação, o **System Services** aparece no menu de aplicativos do sistema na categoria **System**.

### Execução direta (sem instalar)

```bash
java -jar build/libs/system-services-1.0-SNAPSHOT.jar
```

## Estrutura do Projeto

```
src/main/java/SystemServices/
├── App.java                     # Ponto de entrada
├── model/
│   ├── Service.java             # Modelo de serviço systemd
│   └── PortEntry.java           # Modelo de porta/processo
├── service/
│   ├── SystemctlCommand.java    # Wrapper para systemctl
│   └── PortsCommand.java        # Wrapper para ss e kill
└── ui/
    ├── MainWindow.java          # Janela principal, paleta de cores, abas
    ├── ServicesPanel.java       # Painel da aba Serviços
    ├── ServiceTableModel.java   # TableModel para serviços
    ├── ServiceTableRenderer.java
    ├── PortsPanel.java          # Painel da aba Portas
    ├── PortsTableModel.java     # TableModel para portas
    └── PortsTableRenderer.java
```

## Build

```bash
# Compilar e rodar testes
./gradlew build

# Apenas gerar o JAR
./gradlew jar

# Limpar artefatos
./gradlew clean
```
