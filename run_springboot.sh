#!/usr/bin/env bash
# ==============================================================================
#  🚀 Spring Boot Runner v2.0 — Script intelligent de dev Spring Boot
# ==============================================================================
#
#  DESCRIPTION
#  -----------
#  Script universel de compilation, diagnostic et exécution pour n'importe
#  quel projet Spring Boot. Conçu pour les développeurs qui veulent un retour
#  immédiat et actionnable sur leurs erreurs, sans lire des logs bruts.
#
#  FONCTIONNALITÉS
#  ---------------
#  [1] Compilation     → mvn clean compile avec affichage de la progression
#  [2] Diagnostic      → 10 patterns d'erreurs reconnus, expliqués en 3 lignes
#                        + solution en 4 lignes concrètes
#  [3] Gemini AI       → Fallback automatique vers l'API Gemini si l'erreur
#                        n'est pas reconnue (requiert GEMINI_API_KEY)
#  [4] Logs live       → Spring Boot streamé en temps réel avec colorisation :
#                         INFO=blanc  WARN=🟡jaune  ERROR=🔴rouge
#                         HTTP=🔵cyan  SQL=🟢vert  Démarrage=🟢vert gras
#  [5] Surveillance    → Moniteur parallèle qui détecte et signale les erreurs
#                        runtime (HTTP 500, NPE, contraintes SQL) en 4 lignes
#
#  PRÉREQUIS
#  ---------
#  - Java 17+        (java -version)
#  - Maven ou mvnw   (mvn -version / ./mvnw -version)
#  - python3         (pour appel API Gemini — généralement pré-installé)
#  - docker          (optionnel — pour démarrer la base de données)
#
#  USAGE
#  -----
#  ./run_springboot.sh
#
#  CONFIGURATION (fichier .env à créer dans le dossier du projet)
#  ---------------------------------------------------------------
#  GEMINI_API_KEY=votre_clé_api_gemini    # Clé pour le diagnostic Gemini
#  MAVEN_OPTS=-Xmx2048m                   # (optionnel) Mémoire heap Maven
#
#  PATTERNS D'ERREURS RECONNUS (Compilation & Démarrage)
#  -------------------------------------------------------
#   1. Syntaxe Java (javac)          6. Incompatibilité JDK
#   2. Bean / injection Spring        7. Dépendances Maven introuvables
#   3. Conflit de port (8080, etc.)   8. Mémoire insuffisante (OOM)
#   4. Connexion base de données      9. Caused by: (exception Spring)
#   5. Config YAML/Properties        10. Erreur inconnue → Gemini AI
#
#  PATTERNS RUNTIME SURVEILLÉS (Pendant l'exécution)
#  --------------------------------------------------
#   - HTTP 500 Internal Server Error  → localisation dans votre code
#   - NullPointerException            → classe + ligne exacte
#   - DataIntegrityViolationException → détail SQL
#
#  AUTEUR  : Script généré avec Spring Boot Runner v2.0
#  VERSION : 2.0
# ==============================================================================

# ==============================================================================
# SECTION 1 — CONFIGURATION
# ==============================================================================

# --- Chargement des variables depuis .env si le fichier existe ---
if [ -f ".env" ]; then
    set -o allexport
    # shellcheck source=.env
    source .env
    set +o allexport
fi

# Couleurs terminal
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

# Gemini API
GEMINI_API_KEY="${GEMINI_API_KEY:-}"
GEMINI_URL="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

# Configuration automatique de JAVA_HOME vers JDK 21 si présent
if [ -n "${JAVA_HOME:-}" ] && [ -d "$JAVA_HOME/bin" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
elif [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
    export PATH="$JAVA_HOME/bin:$PATH"
elif [ -d "/usr/lib/jvm/openjdk-21" ]; then
    export JAVA_HOME="/usr/lib/jvm/openjdk-21"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# Fichiers de log
LOG_FILE=$(mktemp /tmp/chess-arena-run.XXXXXX.log)
COMPILE_LOG=$(mktemp)
RUNTIME_ERR_TMP=$(mktemp)
# Flag : positionné (touch) dès que Spring Boot a fini de démarrer.
# Utilisé pour supprimer les logs Hibernate durant la phase de boot.
APP_STARTED_FLAG=$(mktemp -u)   # mktemp -u : réserve le nom sans créer le fichier

# Nettoyage automatique des fichiers temporaires à la sortie
trap 'rm -f "$COMPILE_LOG" "$RUNTIME_ERR_TMP" "$APP_STARTED_FLAG" "$LOG_FILE"' EXIT

# ==============================================================================
# SECTION 2 — BANNIÈRE DE DÉMARRAGE
# ==============================================================================

print_banner() {
    # Nom du projet depuis pom.xml ou nom du dossier courant
    local project_name
    project_name=$(grep -m2 '<artifactId>' pom.xml 2>/dev/null \
        | tail -n1 \
        | sed -E 's/.*<artifactId>(.*)<\/artifactId>.*/\1/' \
        || basename "$PWD")

    # Version Java installée
    local java_version
    java_version=$(java -version 2>&1 | grep -oE '"[0-9]+(\.[0-9]+)*"' | tr -d '"' | head -n1)
    java_version="${java_version:-inconnu}"

    # Détection du type de base de données depuis docker-compose.yml
    local db_type="Pas de DB Docker"
    if [ -f "docker-compose.yml" ] || [ -f "compose.yml" ]; then
        local compose_file="docker-compose.yml"
        [ -f "compose.yml" ] && compose_file="compose.yml"
        if   grep -qi "mysql"    "$compose_file"; then db_type="MySQL (Docker)"
        elif grep -qi "postgres" "$compose_file"; then db_type="PostgreSQL (Docker)"
        elif grep -qi "mongo"    "$compose_file"; then db_type="MongoDB (Docker)"
        elif grep -qi "redis"    "$compose_file"; then db_type="Redis (Docker)"
        fi
    fi

    echo -e ""
    echo -e "${BLUE}${BOLD}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}${BOLD}║         🚀 Spring Boot Runner v2.0                       ║${NC}"
    printf  "${BLUE}${BOLD}║  Projet : %-18s  Java %-7s  %-16s  ║${NC}\n" \
            "$project_name" "$java_version" "$db_type"
    echo -e "${BLUE}${BOLD}╚══════════════════════════════════════════════════════════╝${NC}"
    echo -e ""
}

# ==============================================================================
# SECTION 3 — VÉRIFICATIONS PRÉLIMINAIRES
# ==============================================================================

check_java() {
    if ! command -v java &> /dev/null; then
        echo -e "${RED}${BOLD}  ✗ Java introuvable sur ce système.${NC}"
        echo -e "${YELLOW}  → Installez Java 17+ : sudo apt install openjdk-21-jdk${NC}"
        echo -e "${YELLOW}  → Ou téléchargez sur : https://adoptium.net${NC}"
        exit 1
    fi
    local ver
    ver=$(java -version 2>&1 | grep -oE '"[0-9]+(\.[0-9]+)*"' | tr -d '"' | head -n1)
    echo -e "${GREEN}  ✓ Java ${ver} détecté${NC}"
}

check_maven() {
    if [ -f "./mvnw" ]; then
        chmod +x ./mvnw
        MVN_CMD="./mvnw"
        echo -e "${GREEN}  ✓ Maven Wrapper (./mvnw) détecté${NC}"
    elif command -v mvn &> /dev/null; then
        MVN_CMD="mvn"
        echo -e "${GREEN}  ✓ Maven système détecté${NC}"
    else
        echo -e "${RED}${BOLD}  ✗ Maven introuvable (ni ./mvnw ni mvn).${NC}"
        echo -e "${YELLOW}  → Installez Maven : sudo apt install maven${NC}"
        echo -e "${YELLOW}  → Ou générez le wrapper : mvn -N wrapper:wrapper${NC}"
        exit 1
    fi
}

check_docker() {
    # Cherche un fichier compose dans le dossier courant
    local compose_file=""
    [ -f "docker-compose.yml" ] && compose_file="docker-compose.yml"
    [ -f "compose.yml" ]        && compose_file="compose.yml"

    # Pas de fichier compose → on passe
    [ -z "$compose_file" ] && return

    # Docker non installé → avertissement simple
    if ! command -v docker &> /dev/null; then
        echo -e "${YELLOW}  ⚠ docker-compose.yml trouvé mais Docker n'est pas installé.${NC}"
        return
    fi

    # Compter les conteneurs actifs
    local running
    running=$(docker compose ps --status running -q 2>/dev/null | wc -l)

    if [ "$running" -gt 0 ]; then
        echo -e "${GREEN}  ✓ Docker : ${running} conteneur(s) actif(s)${NC}"
    else
        echo -e "${YELLOW}  ⚠ Aucun conteneur Docker actif détecté.${NC}"
        echo -ne "${CYAN}  → Lancer 'docker compose up -d' maintenant ? [O/n] : ${NC}"
        read -r answer
        if [[ "$answer" =~ ^([Oo]|)$ ]]; then
            echo -e "${BLUE}  → Démarrage des conteneurs...${NC}"
            docker compose up -d
            echo -e "${GREEN}  ✓ Conteneurs démarrés. Attente de 5 secondes (init DB)...${NC}"
            sleep 5
        else
            echo -e "${YELLOW}  ⚠ Docker ignoré — risque d'erreur de connexion DB.${NC}"
        fi
    fi
}

run_checks() {
    echo -e "${BLUE}${BOLD}── Vérifications préliminaires ──────────────────────────────${NC}"
    check_java
    check_maven
    check_docker
    echo -e ""
}

# ==============================================================================
# SECTION 3b — GESTION INTERACTIVE DES CONFLITS DE PORT
# ==============================================================================

# Appelé lorsque le port est déjà occupé.
# Propose deux choix : tuer le processus occupant le port, ou relancer sur un autre port.
handle_port_conflict() {
    local port="$1"

    # Identifie le processus occupant le port
    local pid
    pid=$(lsof -ti :"$port" 2>/dev/null | head -n1)
    local proc_name=""
    [ -n "$pid" ] && proc_name=$(ps -p "$pid" -o comm= 2>/dev/null)

    echo -e ""
    echo -e "${RED}${BOLD}  ╔══ ✗ CONFLIT DE PORT ══════════════════════════════════════${NC}"
    echo -e "${RED}  ║   Le port ${port} est déjà utilisé par un autre processus.${NC}"
    echo -e "${RED}  ║   Processus identifié : ${proc_name:-inconnu} (PID ${pid:-?})${NC}"
    echo -e "${RED}  ╚════════════════════════════════════════════════════════════${NC}"
    echo -e ""
    echo -e "${CYAN}${BOLD}  Que souhaitez-vous faire ?${NC}"
    echo -e "${CYAN}  [1] Tuer le processus sur le port ${port} et relancer l'application${NC}"
    echo -e "${CYAN}  [2] Relancer l'application sur un autre port${NC}"
    echo -e "${CYAN}  [q] Quitter${NC}"
    echo -ne "${CYAN}${BOLD}  Votre choix [1/2/q] : ${NC}"
    read -r choice

    case "$choice" in
        1)
            if [ -z "$pid" ]; then
                echo -e "${RED}  ✗ Impossible d'identifier le PID sur le port ${port}. Arrêt.${NC}"
                exit 1
            fi
            echo -e "${YELLOW}  → Arrêt du processus PID ${pid} (${proc_name:-?})...${NC}"
            kill -9 "$pid" 2>/dev/null
            sleep 1
            echo -e "${GREEN}  ✓ Processus tué. Relance de l'application sur le port ${port}...${NC}"
            echo -e ""
            phase_run
            ;;
        2)
            echo -ne "${CYAN}  → Entrez le nouveau port (ex: 8081) : ${NC}"
            read -r new_port
            if ! [[ "$new_port" =~ ^[0-9]+$ ]] || [ "$new_port" -lt 1024 ] || [ "$new_port" -gt 65535 ]; then
                echo -e "${RED}  ✗ Port invalide '${new_port}'. Doit être un entier entre 1024 et 65535.${NC}"
                exit 1
            fi
            echo -e "${GREEN}  ✓ Relance sur le port ${new_port}...${NC}"
            echo -e ""
            # Lance Maven avec le port surchargé via propriété système
            MVN_EXTRA_ARGS="-Dserver.port=${new_port}" phase_run
            ;;
        q|Q)
            echo -e "${YELLOW}  ✓ Arrêt demandé par l'utilisateur.${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}  ✗ Choix invalide. Arrêt.${NC}"
            exit 1
            ;;
    esac
}

# ==============================================================================
# SECTION 4 — DIAGNOSTIC D'ERREURS (Compilation) + FALLBACK GEMINI
# ==============================================================================


# Affiche un bloc d'erreur structuré :
#   - 3 lignes de description du problème
#   - 4 lignes de solution concrète
print_error_box() {
    local phase="$1"
    local p1="$2" p2="$3" p3="$4"   # Problème (max 3 lignes)
    local s1="$5" s2="$6" s3="$7" s4="$8"  # Solution (max 4 lignes)
    local SEP="────────────────────────────────────────────────────────────"

    echo -e ""
    echo -e "${RED}${BOLD}  ╔══ ✗ ERREUR ${phase} ${NC}"
    echo -e "${RED}${BOLD}  ║${NC}"
    echo -e "${RED}${BOLD}  ║  [PROBLÈME]${NC}"
    [ -n "$p1" ] && echo -e "${RED}  ║   → ${p1}${NC}"
    [ -n "$p2" ] && echo -e "${RED}  ║   → ${p2}${NC}"
    [ -n "$p3" ] && echo -e "${RED}  ║   → ${p3}${NC}"
    echo -e "${YELLOW}${BOLD}  ║${NC}"
    echo -e "${YELLOW}${BOLD}  ║  [SOLUTION]${NC}"
    [ -n "$s1" ] && echo -e "${YELLOW}  ║   1. ${s1}${NC}"
    [ -n "$s2" ] && echo -e "${YELLOW}  ║   2. ${s2}${NC}"
    [ -n "$s3" ] && echo -e "${YELLOW}  ║   3. ${s3}${NC}"
    [ -n "$s4" ] && echo -e "${YELLOW}  ║   4. ${s4}${NC}"
    echo -e "${RED}${BOLD}  ╚══ ${SEP:0:40}${NC}"
    echo -e ""
}

# Envoie le log d'erreur à l'API Gemini et affiche la réponse formatée
gemini_fallback() {
    local phase="$1"

    if [ -z "$GEMINI_API_KEY" ]; then
        echo -e "${YELLOW}  ⚠ GEMINI_API_KEY non définie dans .env — diagnostic Gemini indisponible.${NC}"
        echo -e "${YELLOW}  → Ajoutez GEMINI_API_KEY=votre_clé dans le fichier .env${NC}"
        return
    fi

    echo -e "${MAGENTA}${BOLD}  🤖 Erreur non reconnue — Consultation de Gemini AI...${NC}"

    # Extrait les 30 lignes d'erreur les plus significatives
    local log_excerpt
    log_excerpt=$(grep -E "(ERROR|Exception|Caused by|FAILED)" "$COMPILE_LOG" \
        | grep -v "BUILD FAILURE" | head -n 30)

    # Construction du prompt Gemini en JSON sécurisé via python3
    local response
    response=$(python3 -c "
import json, urllib.request, urllib.error, sys

api_key = '${GEMINI_API_KEY}'
log = '''${log_excerpt}'''

prompt = (
    'Tu es un expert Spring Boot et DevOps. '
    'Voici un log d erreur d un projet Spring Boot (phase : ${phase}). '
    'Explique le problème en 3 lignes maximum, puis propose une solution concrète en 4 lignes maximum. '
    'Réponds UNIQUEMENT en français, de façon directe et concise. '
    'Format souhaité :\n[PROBLÈME]\n...\n[SOLUTION]\n...\n\nLog:\n' + log
)

models = ['gemini-2.0-flash', 'gemini-2.0-flash-lite']
success = False

for model in models:
    url = f'https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key=' + api_key
    payload = json.dumps({'contents': [{'parts': [{'text': prompt}]}]}).encode()
    req = urllib.request.Request(url, data=payload, headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read())
            text = data['candidates'][0]['content']['parts'][0]['text']
            print(text.strip())
            success = True
            break
    except urllib.error.HTTPError as e:
        if e.code == 404:
            continue
        elif e.code == 429:
            print('GEMINI_ERROR: Quota API Gemini temporairement dépassé (HTTP 429). Réessayez dans un instant.')
            success = True
            break
        else:
            print(f'GEMINI_ERROR: Erreur HTTP {e.code}: {e.reason}')
            success = True
            break
    except Exception as e:
        print('GEMINI_ERROR: ' + str(e))
        success = True
        break

if not success:
    print('GEMINI_ERROR: Impossible d\'accéder aux modèles Gemini (404/indisponible).')
" 2>/dev/null)

    echo -e ""
    echo -e "${MAGENTA}${BOLD}  ╔══ 🤖 ANALYSE GEMINI AI ══════════════════════════════════${NC}"
    echo -e "${MAGENTA}${BOLD}  ║  Phase : ${phase}${NC}"
    echo -e "${MAGENTA}${BOLD}  ╠════════════════════════════════════════════════════════════${NC}"

    if echo "$response" | grep -q "^GEMINI_ERROR:"; then
        echo -e "${YELLOW}  ║  ⚠ Impossible de joindre Gemini : ${response#GEMINI_ERROR: }${NC}"
        echo -e "${YELLOW}  ║  → Vérifiez votre connexion internet et votre clé API.${NC}"
    else
        while IFS= read -r line; do
            printf "${MAGENTA}  ║${NC}  %s\n" "$line"
        done <<< "$response"
    fi

    echo -e "${MAGENTA}${BOLD}  ╚════════════════════════════════════════════════════════════${NC}"
    echo -e ""
}

# Analyse les logs de compilation/démarrage et appelle le bon diagnostic
analyze_error() {
    local phase="$1"

    # --- 1. ERREURS COMPILATION & SYNTAXE JAVA ---
    if grep -q "\[ERROR\] /" "$COMPILE_LOG"; then
        local file line_num detail
        file=$(grep "\[ERROR\] /" "$COMPILE_LOG" | head -n1 \
            | sed -E 's/.*\/src\/main\/java\///' | cut -d: -f1)
        line_num=$(grep "\[ERROR\] /" "$COMPILE_LOG" | head -n1 \
            | grep -oE ':[0-9]+:' | head -n1 | tr -d ':')
        detail=$(grep "\[ERROR\] /" "$COMPILE_LOG" | head -n1 \
            | sed -E 's/.*\[ERROR\] //' | cut -c1-80)
        print_error_box "$phase" \
            "Erreur de syntaxe Java détectée par le compilateur (javac)." \
            "Fichier : ${file:-inconnu}${line_num:+ — ligne $line_num}" \
            "Message : ${detail:-voir log complet}" \
            "Ouvrez le fichier indiqué et corrigez la ligne signalée." \
            "Vérifiez les accolades { }, parenthèses ( ) et point-virgules." \
            "Vérifiez que tous les imports sont présents et corrects." \
            "Relancez : ./run_springboot.sh"
        return
    fi

    # --- 2. BEAN / INJECTION DE DÉPENDANCES ---
    if grep -q -iE "(NoSuchBeanDefinitionException|UnsatisfiedDependencyException|BeanCreationException)" "$COMPILE_LOG"; then
        local bean
        bean=$(grep -iE "(NoSuchBeanDefinitionException|UnsatisfiedDependencyException)" "$COMPILE_LOG" \
            | head -n1 | grep -oE "'[^']+'" | head -n1)
        print_error_box "$phase" \
            "Spring ne trouve pas un Bean requis pour l'injection de dépendances." \
            "Bean manquant : ${bean:-voir log}. Le contexte Spring ne peut pas démarrer." \
            "Cause probable : annotation manquante ou interface non implémentée." \
            "Vérifiez que la classe est annotée (@Service, @Repository, @Component...)." \
            "Assurez-vous qu'elle est dans un package scanné par Spring Boot." \
            "Vérifiez les interfaces : le type injecté doit correspondre exactement." \
            "Relancez après correction : ./run_springboot.sh"
        return
    fi

    # --- 3. CONFLITS DE PORTS ---
    if grep -q -iE "(Port .* already in use|Address already in use|webServerStartStop)" "$COMPILE_LOG"; then
        local port
        port=$(grep -oE "port [0-9]+" "$COMPILE_LOG" | head -n1 | grep -oE "[0-9]+")
        port="${port:-8080}"
        handle_port_conflict "$port"
        return
    fi

    # --- 4. BASE DE DONNÉES & CONNEXION ---
    if grep -q -iE "(FlywayException|PSQLException|CommunicationsException|Connection refused|HikariPool.*Exception|Cannot create PoolableConnectionFactory)" "$COMPILE_LOG"; then
        local db_detail
        db_detail=$(grep -iE "(Connection refused|Communications link failure|PSQLException|FlywayException)" \
            "$COMPILE_LOG" | head -n1 | sed -E 's/.*: //' | cut -c1-70)
        print_error_box "$phase" \
            "Impossible de se connecter à la base de données." \
            "Le pool de connexions HikariCP a échoué à établir la connexion." \
            "Détail : ${db_detail:-serveur DB inaccessible ou identifiants incorrects}" \
            "Vérifiez que le serveur DB est démarré (ex: docker compose up -d)." \
            "Contrôlez les paramètres dans application.yml (url, username, password)." \
            "Testez la connexion : psql -U findme_user -h localhost -d findme" \
            "Relancez après correction : ./run_springboot.sh"
        return
    fi

    # --- 5. ERREURS DE CONFIGURATION (YAML / Properties) ---
    if grep -q -iE "(YamlParseException|ParserException|ScannerException|InvalidPropertyException|BindException)" "$COMPILE_LOG"; then
        local yaml_detail
        yaml_detail=$(grep -iE "(YamlParseException|ParserException|ScannerException|BindException)" \
            "$COMPILE_LOG" | head -n1 | sed -E 's/.*: //' | cut -c1-70)
        print_error_box "$phase" \
            "Erreur de syntaxe ou de valeur dans un fichier de configuration." \
            "Spring Boot ne peut pas charger application.yml ou application.properties." \
            "Détail : ${yaml_detail:-indentation YAML incorrecte ou propriété invalide}" \
            "Ouvrez src/main/resources/application.yml et vérifiez l'indentation." \
            "Chaque niveau YAML doit être indenté de 2 espaces (pas de tabulations)." \
            "Vérifiez que les valeurs correspondent aux types attendus (int, bool...)." \
            "Relancez après correction : ./run_springboot.sh"
        return
    fi

    # --- 6. INCOMPATIBILITÉ VERSION JDK ---
    if grep -q -iE "(UnsupportedClassVersionError|has been compiled by a more recent version)" "$COMPILE_LOG"; then
        local jdk_ver
        jdk_ver=$(java -version 2>&1 | head -n1)
        print_error_box "$phase" \
            "Le bytecode Java compilé est incompatible avec la JVM utilisée." \
            "JVM actuelle : ${jdk_ver}." \
            "Le pom.xml exige une version différente de Java." \
            "Exécutez : java -version pour voir votre version actuelle." \
            "Vérifiez <java.version> dans pom.xml et alignez votre JDK." \
            "Installez la bonne version : sudo apt install openjdk-21-jdk" \
            "Définissez JAVA_HOME vers la bonne version et relancez."
        return
    fi

    # --- 7. DÉPENDANCES MAVEN INTROUVABLES ---
    if grep -q -iE "(DependencyResolutionException|Could not resolve dependencies|PluginResolutionException|Could not find artifact)" "$COMPILE_LOG"; then
        local dep_detail
        dep_detail=$(grep -iE "(Could not find artifact|Could not resolve)" \
            "$COMPILE_LOG" | head -n1 | sed -E 's/.*\[ERROR\] //' | cut -c1-70)
        print_error_box "$phase" \
            "Maven ne peut pas télécharger une ou plusieurs dépendances." \
            "Artefact manquant : ${dep_detail:-voir pom.xml}" \
            "Cause : dépôt Maven inaccessible, version inexistante ou pom.xml corrompu." \
            "Vérifiez votre connexion internet (proxy, firewall)." \
            "Supprimez le cache : rm -rf ~/.m2/repository/<groupId> et relancez." \
            "Vérifiez la version de la dépendance sur https://mvnrepository.com" \
            "Relancez : ./run_springboot.sh"
        return
    fi

    # --- 8. ERREURS MÉMOIRE ---
    if grep -q -iE "(OutOfMemoryError|Java heap space|GC overhead limit)" "$COMPILE_LOG"; then
        print_error_box "$phase" \
            "La JVM manque de mémoire heap (java.lang.OutOfMemoryError)." \
            "Maven ou l'application consomme plus de RAM que la limite configurée." \
            "Problème fréquent sur les machines avec moins de 4 Go de RAM libre." \
            "Augmentez la mémoire Maven : export MAVEN_OPTS=\"-Xmx2048m\"" \
            "Ou ajoutez dans .env : MAVEN_OPTS=-Xmx2048m" \
            "Fermez les applications gourmandes en RAM puis relancez." \
            "Relancez : ./run_springboot.sh"
        return
    fi

    # --- 8b. SPRING SECURITY NON CONFIGURÉ ---
    if grep -q -iE "(UserDetailsService|InMemoryUserDetailsManager|SecurityFilterChain|HttpSecurity|defaultSecurityFilterChain|Spring Security)" "$COMPILE_LOG"; then
        print_error_box "$phase" \
            "Spring Security est dans le classpath mais n'est pas configuré." \
            "Aucune classe @Configuration de sécurité n'a été trouvée par Spring Boot." \
            "L'application tente d'activer la sécurité par défaut et échoue." \
            "Créez une classe annotée @Configuration @EnableWebSecurity étendant SecurityFilterChain." \
            "Ou excluez Spring Security du pom.xml si vous n'en avez pas besoin." \
            "Ou ajoutez dans application.properties : spring.autoconfigure.exclude=...SecurityAutoConfiguration"
        return
    fi

    # --- 9. CAPTURE UNIVERSELLE : Caused by connu ---
    if grep -q "Caused by:" "$COMPILE_LOG"; then
        local root_cause
        root_cause=$(grep "Caused by:" "$COMPILE_LOG" | tail -n1 | sed -E 's/Caused by: //' | cut -c1-80)
        print_error_box "$phase" \
            "Exception Spring non reconnue par les patterns standards." \
            "Cause racine détectée : ${root_cause}" \
            "Le contexte d'application Spring Boot n'a pas pu démarrer." \
            "Lisez la stack trace complète dans run.log pour le contexte." \
            "Vérifiez vos annotations (@Bean, @Autowired, @Configuration...)." \
            "Cherchez '${root_cause:0:40}' dans votre code source." \
            "Si bloqué, Gemini sera consulté automatiquement."
        # Appel Gemini pour enrichir le diagnostic
        gemini_fallback "$phase"
        return
    fi

    # --- 10. ERREUR INCONNUE → GEMINI OBLIGATOIRE ---
    local raw_err
    raw_err=$(grep -E "^\[ERROR\]" "$COMPILE_LOG" | grep -v "BUILD FAILURE" \
        | head -n1 | sed -E 's/\[ERROR\] //')
    print_error_box "$phase" \
        "Erreur non reconnue par les diagnostics intégrés." \
        "Détail brut : ${raw_err:-aucun message [ERROR] trouvé dans le log}" \
        "Gemini AI va analyser le log complet pour identifier le problème." \
        "Consultez run.log pour le détail complet de l'erreur." \
        "Vérifiez que votre pom.xml ne contient pas de caractères invalides." \
        "Assurez-vous que votre environnement Java est correctement configuré." \
        "Suivez les recommandations de Gemini ci-dessous."
    gemini_fallback "$phase"
}

# ==============================================================================
# SECTION 5 — COMPILATION
# ==============================================================================

phase_compile() {
    echo -e "${BLUE}${BOLD}── [1/2] Compilation (mvn clean compile) ───────────────────${NC}"

    if ! $MVN_CMD clean compile > "$COMPILE_LOG" 2>&1; then
        analyze_error "COMPILATION"
        exit 1
    fi

    echo -e "${GREEN}${BOLD}  ✓ Compilation réussie.${NC}"
    echo -e ""
}

# ==============================================================================
# SECTION 6 — EXÉCUTION : LOGS LIVE + SURVEILLANCE RUNTIME
# ==============================================================================

# Colorise une ligne de log Spring Boot selon son niveau
colorize_log_line() {
    local line="$1"

    # --- Filtre : lignes Maven build et Java 26 warnings (bruit à ignorer) ---
    # On n'affiche que les vraies lignes Spring Boot (après le banner ascii)
    if echo "$line" | grep -qE "^(\[INFO\]|\[WARNING\]|WARNING:|\[ERROR\] BUILD|\[ERROR\] To see)"; then
        # Sauvegarde dans le log mais n'affiche pas dans le terminal
        return
    fi
    # Filtre les warnings JVM de Java 26 (méthodes restreintes, Unsafe, etc.)
    if echo "$line" | grep -qE "(restricted method|terminally deprecated|JansiLoader|objectFieldOffset|final field.*mutated|enable-native-access|enable-final-field-mutation)"; then
        return
    fi
    # Filtre les lignes Maven scanning/building/compiling de spring-boot:run
    if echo "$line" | grep -qE "^\[INFO\] (Scanning|Building|---|\.   __|Attaching|Nothing to compile|Copying|skip non|Recompiling|Compiling|>>>|<<<|$)"; then
        return
    fi

    # --- Filtre : rapport d'auto-configuration Spring Boot (CONDITIONS REPORT) ---
    # Ces lignes apparaissent en mode debug (debug=true dans application.yml).
    # Le rapport liste TOUTES les auto-configurations, qu'elles aient matché ou non.
    #
    # Patterns couverts :
    #   - En-têtes du rapport   : CONDITIONS EVALUATION REPORT, Positive matches, etc.
    #   - Lignes de classes     : "   XxxAutoConfiguration matched:" (3+ espaces)
    #   - Lignes de conditions  : "      - @ConditionalOn...", "      - Initialized..."
    #   - Lignes de détail      : NoneNestedConditions, NestedCondition on, etc.

    # En-têtes et mots-clés principaux du rapport
    if echo "$line" | grep -qiE "(CONDITIONS EVALUATION REPORT|Positive matches:|Negative matches:|Exclusions:|Unconditional classes:)"; then
        return
    fi
    # Toute ligne contenant "matched:" ou "did not match:" (fin de ligne ou milieu)
    if echo "$line" | grep -qiE "(\bmatched:\s*$|did not match:)"; then
        return
    fi
    # Lignes de conditions multi-lignes (indentées, avec tiret)
    if echo "$line" | grep -qiE "^[[:space:]]{4,}-[[:space:]]+(NoneNested|NestedCondition|@|Initialized|Management|Logback|[A-Z][a-z]+[A-Z])"; then
        return
    fi
    # Toute ligne indentée contenant des annotations @ConditionalOn, condition names, etc.
    if echo "$line" | grep -qiE "(@ConditionalOn|OnClassCondition|OnPropertyCondition|OnBeanCondition|OnWebApplicationCondition|OnManagementPort)"; then
        return
    fi
    # Lignes « XxxAutoConfiguration matched: » ou « Xxx#methodName matched: »
    if echo "$line" | grep -qiE "^[[:space:]]{2,}[A-Z][A-Za-z0-9#.]+[[:space:]]+(matched:|did not match:)"; then
        return
    fi
    # Lignes de continuation du rapport (indentées avec "-" + détail)
    if echo "$line" | grep -qiE "^[[:space:]]{5,}-[[:space:]]+[A-Za-z]"; then
        return
    fi

    # --- Filtre anti-flood : requêtes Hibernate identiques répétées ---
    # RÈGLE : les logs Hibernate sont supprimés pendant le démarrage.
    # Ils ne s'affichent qu'APRÈS que l'app est pleinement démarrée
    # (= quand l'utilisateur envoie une requête HTTP).
    if echo "$line" | grep -qiE "^Hibernate:\s+(select|insert|update|delete)"; then
        # Pendant le boot → silence total
        if [ ! -f "${APP_STARTED_FLAG}" ]; then
            return
        fi
        # Après démarrage → déduplique les requêtes identiques (anti N+1 flood)
        local sql_sig
        sql_sig=$(echo "$line" | sed -E 's/=[?]/=?/g' | cut -c1-80)
        if [ "$sql_sig" = "${_LAST_SQL:-}" ]; then
            _SQL_COUNT=$(( ${_SQL_COUNT:-1} + 1 ))
            if [ "${_SQL_COUNT}" -eq 3 ]; then
                echo -e "${YELLOW}${BOLD}  ⚠ [N+1 DÉTECTÉ] Même requête SQL répétée (${_SQL_COUNT}x et +).${NC}"
                echo -e "${YELLOW}  │  Requête : ${sql_sig:0:70}${NC}"
                echo -e "${YELLOW}  │  Cause   : Boucle sans JOIN FETCH ou @BatchSize dans votre repository.${NC}"
                echo -e "${YELLOW}  │  Action  : Ajoutez JOIN FETCH ou @Query avec FETCH dans votre méthode.${NC}"
            fi
            return
        else
            _LAST_SQL="$sql_sig"
            _SQL_COUNT=1
        fi
    else
        _LAST_SQL=""
        _SQL_COUNT=0
    fi

    # Erreurs (rouge) — priorité max
    if echo "$line" | grep -qE "\sERROR\s"; then
        echo -e "${RED}${line}${NC}"

    # Avertissements Spring Boot (jaune)
    elif echo "$line" | grep -qE "\sWARN\s"; then
        echo -e "${YELLOW}${line}${NC}"

    # Requêtes HTTP (cyan) — GET, POST, PUT, DELETE, PATCH
    elif echo "$line" | grep -qE "(GET|POST|PUT|DELETE|PATCH)\s+/"; then
        echo -e "${CYAN}${line}${NC}"

    # SQL Hibernate (vert) — affiché uniquement après démarrage (filtre ci-dessus)
    elif echo "$line" | grep -qiE "^\s*(select|insert|update|delete)\s"; then
        echo -e "${GREEN}${DIM}${line}${NC}"

    # Démarrage réussi Spring Boot → active l'affichage des logs Hibernate
    elif echo "$line" | grep -qE "Started .* in [0-9]"; then
        touch "${APP_STARTED_FLAG}"   # ← signal : app prête, Hibernate autorisé
        echo -e "${GREEN}${BOLD}${line}${NC}"
        echo -e ""
        echo -e "${GREEN}${BOLD}  ✓ Application prête — en attente de requêtes HTTP${NC}"
        echo -e ""

    # INFO standard Spring Boot (blanc/normal)
    elif echo "$line" | grep -qE "\sINFO\s"; then
        echo -e "${line}"

    # DEBUG (grisé)
    elif echo "$line" | grep -qE "\sDEBUG\s"; then
        echo -e "${DIM}${line}${NC}"

    # Banner Spring Boot, stack traces et autres lignes
    elif [ -n "$line" ]; then
        echo -e "${DIM}${line}${NC}"
    fi
}

# Surveille le fichier de log en temps réel et détecte les erreurs runtime
# Appelle l'alerte si une erreur critique est détectée
watch_runtime_errors() {
    local log_pipe="$1"

    tail -f "$log_pipe" 2>/dev/null | while IFS= read -r line; do

        # --- Détection : HTTP 500 Internal Server Error ---
        if echo "$line" | grep -qiE "(500 Internal Server Error|Resolved \[.*Exception|ERROR.*DispatcherServlet)"; then

            # Cherche la classe et méthode incriminée dans les 20 dernières lignes du log
            local last_lines
            last_lines=$(tail -n 20 "$log_pipe" 2>/dev/null)

            local class_info
            class_info=$(echo "$last_lines" \
                | grep -oE "at com\.[a-zA-Z0-9.]+\([A-Za-z]+\.java:[0-9]+\)" \
                | grep -v "springframework\|hibernate\|tomcat\|sun\." \
                | head -n1)

            local exception_type
            exception_type=$(echo "$last_lines" \
                | grep -oE "(NullPointerException|EntityNotFoundException|DataIntegrityViolationException|IllegalArgumentException|MethodArgumentNotValidException|HttpMessageNotReadableException|NoSuchElementException|AccessDeniedException)[^:]*" \
                | head -n1)
            exception_type="${exception_type:-Exception inconnue}"

            echo -e "" >&2
            echo -e "${RED}${BOLD}  ┌─ 🔴 ERREUR RUNTIME DÉTECTÉE ──────────────────────────────┐${NC}" >&2
            echo -e "${RED}  │  Type    : ${exception_type}${NC}" >&2
            if [ -n "$class_info" ]; then
                echo -e "${RED}  │  Où agir : ${class_info}${NC}" >&2
            else
                echo -e "${RED}  │  Où agir : Vérifiez les controllers et services récemment modifiés${NC}" >&2
            fi
            echo -e "${YELLOW}  │  Cause   : Requête HTTP traitée qui a provoqué une exception non gérée${NC}" >&2
            echo -e "${YELLOW}  │  Action  : Ajoutez un @ExceptionHandler ou vérifiez vos données d'entrée${NC}" >&2
            echo -e "${RED}${BOLD}  └────────────────────────────────────────────────────────────┘${NC}" >&2
            echo -e "" >&2

        # --- Détection : NullPointerException ---
        elif echo "$line" | grep -qE "NullPointerException"; then
            local npe_location
            npe_location=$(tail -n 10 "$log_pipe" 2>/dev/null \
                | grep -oE "at com\.[a-zA-Z0-9.]+\([A-Za-z]+\.java:[0-9]+\)" \
                | grep -v "springframework\|hibernate" | head -n1)
            echo -e "" >&2
            echo -e "${RED}${BOLD}  ┌─ ⚠ NullPointerException ───────────────────────────────────┐${NC}" >&2
            echo -e "${RED}  │  Type    : java.lang.NullPointerException${NC}" >&2
            echo -e "${RED}  │  Où agir : ${npe_location:-classe inconnue — voir stack trace}${NC}" >&2
            echo -e "${YELLOW}  │  Cause   : Un objet non initialisé a été utilisé sans vérification null${NC}" >&2
            echo -e "${YELLOW}  │  Action  : Ajoutez une vérification null ou utilisez Optional<>${NC}" >&2
            echo -e "${RED}${BOLD}  └────────────────────────────────────────────────────────────┘${NC}" >&2
            echo -e "" >&2

        # --- Détection : Erreur SQL / Intégrité de données ---
        elif echo "$line" | grep -qiE "(DataIntegrityViolationException|SQLIntegrityConstraintViolation|Duplicate entry|foreign key constraint)"; then
            local sql_detail
            sql_detail=$(echo "$line" | grep -oE "(Duplicate entry [^;]+|foreign key constraint [^;]+|Column '[^']+' cannot be null)" | head -n1)
            echo -e "" >&2
            echo -e "${RED}${BOLD}  ┌─ ⚠ ERREUR CONTRAINTE SQL ──────────────────────────────────┐${NC}" >&2
            echo -e "${RED}  │  Type    : DataIntegrityViolationException${NC}" >&2
            echo -e "${RED}  │  Détail  : ${sql_detail:-contrainte SQL violée — voir stack trace}${NC}" >&2
            echo -e "${YELLOW}  │  Cause   : Valeur dupliquée, clé étrangère invalide ou champ NOT NULL vide${NC}" >&2
            echo -e "${YELLOW}  │  Action  : Vérifiez les données envoyées et vos contraintes @Column${NC}" >&2
            echo -e "${RED}${BOLD}  └────────────────────────────────────────────────────────────┘${NC}" >&2
            echo -e "" >&2
        fi

    done
}

# Analyse les logs de démarrage Spring Boot pour détecter un crash au boot
analyze_startup_log() {
    if grep -qE "(APPLICATION FAILED TO START|Error starting)" "$LOG_FILE" 2>/dev/null; then
        # Copie dans COMPILE_LOG pour que analyze_error puisse le lire
        cp "$LOG_FILE" "$COMPILE_LOG"
        analyze_error "DÉMARRAGE"
        return 1
    fi
    return 0
}

phase_run() {
    echo -e "${BLUE}${BOLD}── [2/2] Démarrage de l'application ─────────────────────────${NC}"
    echo -e "${DIM}  Commande : $MVN_CMD spring-boot:run $MVN_EXTRA_ARGS${NC}"
    echo -e ""

    rm -f "$APP_STARTED_FLAG"
    > "$LOG_FILE"

    watch_runtime_errors "$LOG_FILE" &
    local watcher_pid=$!

    $MVN_CMD spring-boot:run $MVN_EXTRA_ARGS 2>&1 | while IFS= read -r line; do
        echo "$line" >> "$LOG_FILE"
        colorize_log_line "$line"
    done
    local maven_exit=${PIPESTATUS[0]}

    kill $watcher_pid 2>/dev/null

    echo -e ""

    # Exit 130 → arrêt voulu par l'utilisateur
    if [ "${maven_exit}" -eq 130 ] 2>/dev/null; then
        echo -e "${YELLOW}  ✓ Application arrêtée (Ctrl+C).${NC}"

    # Exit 0 → arrêt propre (normal)
    elif [ "${maven_exit}" -eq 0 ] 2>/dev/null; then
        echo -e "${GREEN}  ✓ Application arrêtée proprement.${NC}"

    # Autre code → crash ou erreur
    else
        echo -e "${RED}${BOLD}  ✗ L'application s'est arrêtée (code : ${maven_exit})${NC}"
        analyze_startup_log || exit 1
    fi
}

# ==============================================================================
# SECTION 7 — GESTION ET EXÉCUTION DES TESTS
# ==============================================================================

display_test_recap() {
    local test_log="$1"
    local exit_code="$2"

    if [ -f "test_recap.py" ]; then
        python3 test_recap.py "$test_log" "$exit_code"
    fi
}

run_maven_tests() {
    local target_tests="$1"
    local mvn_test_cmd="$MVN_CMD test"

    if [ -n "$target_tests" ]; then
        mvn_test_cmd="$mvn_test_cmd -Dtest=$target_tests"
    fi

    echo -e "${BLUE}${BOLD}── Exécution des tests Maven ───────────────────────────────${NC}"
    echo -e "${DIM}  Commande : $mvn_test_cmd${NC}"
    echo -e ""

    local TEST_LOG
    TEST_LOG=$(mktemp)

    rm -rf target/surefire-reports/TEST-*.xml

    $mvn_test_cmd 2>&1 | tee "$TEST_LOG"
    local _ps=("${PIPESTATUS[@]}")
    local test_exit="${_ps[0]}"

    echo -e ""

    display_test_recap "$TEST_LOG" "$test_exit"

    if [ "$test_exit" -ne 0 ]; then
        cp "$TEST_LOG" "$COMPILE_LOG"
        analyze_error "TESTS"
    fi

    rm -f "$TEST_LOG"
}

phase_tests() {
    echo -e "${BLUE}${BOLD}── Mode Tests ───────────────────────────────────────────────${NC}"
    echo -ne "${CYAN}${BOLD}  Souhaitez-vous lancer TOUS les tests ? [O/n] : ${NC}"
    read -r all_tests_choice

    if [[ "$all_tests_choice" =~ ^([Oo]|)$ ]]; then
        echo -e "${BLUE}  → Lancement de TOUS les tests...${NC}"
        echo -e ""
        run_maven_tests ""
    else
        local test_files=()
        while IFS= read -r file; do
            [ -n "$file" ] && test_files+=("$file")
        done < <(find src/test/java -type f -name "*.java" 2>/dev/null | sort)

        if [ ${#test_files[@]} -eq 0 ]; then
            echo -e "${YELLOW}  ⚠ Aucun fichier de test (.java) trouvé dans src/test/java.${NC}"
            return
        fi

        echo -e ""
        echo -e "${CYAN}${BOLD}── Fichiers de tests disponibles dans src/test/java ─────────${NC}"

        local i=1
        local test_classes=()
        for tf in "${test_files[@]}"; do
            local rel_path="${tf#src/test/java/}"
            local class_fqcn="${rel_path%.java}"
            class_fqcn="${class_fqcn//\//.}"
            local simple_name
            simple_name=$(basename "$tf" .java)

            test_classes+=("$simple_name")
            printf "${CYAN}  [%d] ${BOLD}%-35s${NC} ${DIM}(%s)${NC}\n" "$i" "$simple_name" "$class_fqcn"
            ((i++))
        done

        echo -e ""
        echo -ne "${CYAN}${BOLD}  Sélectionnez le(s) test(s) à exécuter (ex: 1 2 ou 1,3 ou 'all') : ${NC}"
        read -r selection

        if [[ "$selection" == "all" || "$selection" == "ALL" ]]; then
            run_maven_tests ""
            return
        fi

        # Remplacement des virgules par des espaces
        selection="${selection//,/ }"
        local selected_classes=()

        for idx in $selection; do
            if [[ "$idx" =~ ^[0-9]+$ ]] && [ "$idx" -ge 1 ] && [ "$idx" -le "${#test_classes[@]}" ]; then
                selected_classes+=("${test_classes[$((idx - 1))]}")
            else
                echo -e "${YELLOW}  ⚠ Numéro '$idx' ignoré (hors limites).${NC}"
            fi
        done

        if [ ${#selected_classes[@]} -eq 0 ]; then
            echo -e "${RED}  ✗ Aucune sélection valide effectuée. Annulation.${NC}"
            return
        fi

        local test_arg
        test_arg=$(IFS=,; echo "${selected_classes[*]}")

        echo -e "${GREEN}  ✓ Test(s) retenu(s) : ${test_arg}${NC}"
        echo -e ""
        run_maven_tests "$test_arg"
    fi
}

# ==============================================================================
# POINT D'ENTRÉE PRINCIPAL
# ==============================================================================

print_banner
run_checks

echo -e "${BLUE}${BOLD}── Menu Principal ───────────────────────────────────────────${NC}"
echo -e "${CYAN}  [1] 🚀 Lancer l'application (workflow habituel)${NC}"
echo -e "${CYAN}  [2] 🧪 Lancer les tests (tous ou sélection par fichier)${NC}"
echo -e "${CYAN}  [q] ❌ Quitter${NC}"
echo -ne "${CYAN}${BOLD}  Votre choix [1/2/q] : ${NC}"
read -r main_choice

case "$main_choice" in
    1)
        phase_compile
        phase_run
        ;;
    2)
        phase_tests
        ;;
    q|Q)
        echo -e "${YELLOW}  ✓ Arrêt demandé par l'utilisateur.${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}  ✗ Choix invalide. Arrêt.${NC}"
        exit 1
        ;;
esac