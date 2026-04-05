let selectedEndpointUrl = "";
let selectedEndpointPath = "";
let latestResponseBody = "";
let latestRawResponseBody = "";
let latestRequestSequence = 0;
let cachedMetricCatalogNames = [];
const SNAPSHOT_REFRESH_BUTTON_SELECTOR = "#btn-actuator-snapshot-refresh";
const REDIS_SUMMARY_REFRESH_BUTTON_SELECTOR = "#btn-redis-summary-refresh";
const REDIS_INSPECTOR_CARD_SELECTOR = "#redis-inspector-card";
const ENDPOINTS_TAB_SELECTOR = "#actuator-endpoints-tab";
const OPERATIONS_TAB_SELECTOR = "#actuator-operations-tab";
const DEFAULT_ACTUATOR_BASE = "/actuator";
const BULK_DELETE_CONFIRMATION_THRESHOLD = 20;
const MAX_TREND_POINTS = 24;

const trendSeries = {
    wallet: [],
    apiLatency: [],
    dbUtilization: [],
    redisLatency: [],
    redisUsedMemoryPercent: [],
    redisTotalKeys: []
};

const fallbackThresholds = {
    walletFailureWarnPercent: 2,
    walletFailureCriticalPercent: 5,
    apiLatencyWarnMs: 400,
    apiLatencyCriticalMs: 1000,
    apiServerErrorWarnPercent: 1,
    apiServerErrorCriticalPercent: 3,
    dbUtilizationWarnPercent: 80,
    dbUtilizationCriticalPercent: 95,
    dbPendingWarn: 1,
    dbPendingCritical: 10,
    redisLatencyWarnMs: 25,
    redisLatencyCriticalMs: 100,
    redisFailureWarnPercent: 1,
    redisFailureCriticalPercent: 3
};

function init() {
    initializeActuatorViewerState();
    initializeSnapshotState();
    initializeRedisInspectorState();
}

function bind() {
    bindActuatorViewerEvents();
    bindSnapshotEvents();
    bindRedisInspectorEvents();
    bindTabSelectionEvents();
    loadCurrentActiveTabData();
}

function initializeActuatorViewerState() {
    toggleRawButton(false);
    renderViewerMeta(0, "unknown", 0, {
        mode: "text",
        rawContent: "",
        prettyContent: ""
    });
}

function initializeSnapshotState() {
    setSnapshotValue("#snapshot-wallet-tpm", "-", "Waiting for data");
    setSnapshotValue("#snapshot-api-latency", "-", "http.server.requests");
    setSnapshotValue("#snapshot-db-pool", "-", "hikaricp.connections.*");
    setSnapshotValue("#snapshot-redis-latency", "-", "redis.command");
    setSnapshotState("#snapshot-wallet-state", "UNKNOWN");
    setSnapshotState("#snapshot-api-state", "UNKNOWN");
    setSnapshotState("#snapshot-db-state", "UNKNOWN");
    setSnapshotState("#snapshot-redis-state", "UNKNOWN");
    setSnapshotThreshold("#snapshot-wallet-threshold", "Threshold: -");
    setSnapshotThreshold("#snapshot-api-threshold", "Threshold: -");
    setSnapshotThreshold("#snapshot-db-threshold", "Threshold: -");
    setSnapshotThreshold("#snapshot-redis-threshold", "Threshold: -");
    setOverallSnapshotState("UNKNOWN");
    renderTrend("wallet", "#snapshot-wallet-trend");
    renderTrend("apiLatency", "#snapshot-api-trend");
    renderTrend("dbUtilization", "#snapshot-db-trend");
    renderTrend("redisLatency", "#snapshot-redis-trend");
    $("#snapshot-fetched-at").text("Last snapshot: -");
}

function initializeRedisInspectorState() {
    if (!isRedisInspectorAvailable()) {
        return;
    }

    const $limit = $("#redis-key-limit");
    if ($limit.length > 0 && $.fn && typeof $.fn.selectpicker === "function") {
        if ($limit.parent(".bootstrap-select").length === 0) {
            $limit.selectpicker();
        }
        else {
            $limit.selectpicker("refresh");
        }
    }

    setRedisSummaryValue("#redis-summary-used-memory", "-", "-");
    setRedisSummaryValue("#redis-summary-max-memory", "-", "-");
    setRedisSummaryValue("#redis-summary-remaining-memory", "-", "-");
    setRedisSummaryValue("#redis-summary-total-keys", "-", "-");
    $("#redis-summary-status")
        .removeClass("badge-success badge-warning badge-danger")
        .addClass("badge-secondary")
        .text("Not Loaded");
    $("#redis-summary-fetched-at").text("Last summary: -");
    $("#redis-key-table-meta").text("Keys: -");
    renderTrend("redisUsedMemoryPercent", "#redis-summary-used-memory-trend");
    renderTrend("redisTotalKeys", "#redis-summary-total-keys-trend");
    setRedisDeleteButtonState();
    applyRedisPermissionVisibility();
}

function bindActuatorViewerEvents() {
    $("#actuator-endpoint-list").on("click", ".js-actuator-endpoint", function () {
        const endpointUrl = $(this).attr("data-endpoint");
        const endpointPath = $(this).attr("data-endpoint-path");

        activateEndpointButton($(this));
        loadActuatorEndpoint(endpointUrl, endpointPath);
    });

    $("#btn-actuator-refresh").on("click", function () {
        if (!isNotEmpty(selectedEndpointUrl)) {
            return;
        }
        loadActuatorEndpoint(selectedEndpointUrl, selectedEndpointPath);
    });

    $("#btn-actuator-open-raw").on("click", function (e) {
        if (!isNotEmpty(selectedEndpointUrl)) {
            e.preventDefault();
        }
    });

    $("#btn-actuator-copy").on("click", function () {
        copyViewerResponse();
    });

    $("#metrics-catalog-filter").on("input", function () {
        applyMetricsCatalogFilter($(this).val());
    });

    $("#metrics-catalog-body").on("click", ".js-open-metric-from-catalog", function () {
        const metricName = $(this).attr("data-metric-name");
        openMetricFromCatalog(metricName);
    });
}

function bindTabSelectionEvents() {
    $(ENDPOINTS_TAB_SELECTOR).on("shown.bs.tab", function () {
        handleEndpointsTabSelected();
    });

    $(OPERATIONS_TAB_SELECTOR).on("shown.bs.tab", function () {
        handleOperationsTabSelected();
    });
}

function loadCurrentActiveTabData() {
    if ($(OPERATIONS_TAB_SELECTOR).hasClass("active")) {
        handleOperationsTabSelected();
        return;
    }
    handleEndpointsTabSelected();
}

function handleEndpointsTabSelected() {
    if (isNotEmpty(selectedEndpointUrl)) {
        loadActuatorEndpoint(selectedEndpointUrl, selectedEndpointPath);
        return;
    }
    autoLoadFirstEndpoint();
}

function handleOperationsTabSelected() {
    refreshOperationalSnapshot(false);
    refreshRedisSummary(false);
    searchRedisKeys(false);
}

function bindSnapshotEvents() {
    $(SNAPSHOT_REFRESH_BUTTON_SELECTOR).on("click", function () {
        refreshOperationalSnapshot(true);
    });

    $(document).on("click", ".js-open-endpoint", function () {
        const endpointPath = String($(this).attr("data-endpoint-path") || "").trim();
        if (!isNotEmpty(endpointPath)) {
            return;
        }
        openEndpointByPath(endpointPath);
    });
}

function bindRedisInspectorEvents() {
    if (!isRedisInspectorAvailable()) {
        return;
    }

    $(REDIS_SUMMARY_REFRESH_BUTTON_SELECTOR).on("click", function () {
        refreshRedisSummary(true);
    });

    $("#btn-redis-search").on("click", function () {
        searchRedisKeys(true);
    });

    $("#btn-redis-clear").on("click", function () {
        $("#redis-key-pattern").val("");
        searchRedisKeys(true);
    });

    $("#redis-key-limit").on("change", function () {
        searchRedisKeys(false);
    });

    $("#redis-key-pattern").on("keydown", function (event) {
        if (event && event.key === "Enter") {
            event.preventDefault();
            searchRedisKeys(true);
        }
    });

    $("#redis-select-all").on("change", function () {
        const checked = !!$(this).prop("checked");
        $("#redis-key-table-body .js-redis-row-check").prop("checked", checked);
        setRedisDeleteButtonState();
    });

    $("#redis-key-table-body").on("change", ".js-redis-row-check", function () {
        setRedisDeleteButtonState();
    });

    $("#redis-key-table-body").on("click", ".js-redis-delete-one", function () {
        if (!toBoolean(getRedisInspectorConfig().canDelete)) {
            return;
        }
        const key = String($(this).attr("data-key") || "").trim();
        if (!isNotEmpty(key)) {
            return;
        }
        if (!window.confirm("Delete Redis key?\n" + key)) {
            return;
        }
        deleteRedisKeys([key], "");
    });

    $("#btn-redis-delete-selected").on("click", function () {
        if (!toBoolean(getRedisInspectorConfig().canDelete)) {
            return;
        }

        const selectedKeys = [];
        $("#redis-key-table-body .js-redis-row-check:checked").each(function () {
            const key = String($(this).attr("data-key") || "").trim();
            if (isNotEmpty(key)) {
                selectedKeys.push(key);
            }
        });

        if (selectedKeys.length === 0) {
            notify("Info", "Select at least one Redis key to delete.", "warning");
            return;
        }

        if (!window.confirm("Delete " + selectedKeys.length + " selected Redis key(s)?")) {
            return;
        }

        const confirmation = requestBulkDeleteConfirmation(selectedKeys.length);
        if (confirmation === null) {
            return;
        }

        deleteRedisKeys(selectedKeys, confirmation);
    });
}

function autoLoadFirstEndpoint() {
    const firstEndpoint = $("#actuator-endpoint-list .js-actuator-endpoint").first();

    if (firstEndpoint.length === 0) {
        $("#actuator-no-endpoint").removeClass("d-none");
        $("#actuator-response").text("No endpoint permission assigned to your current role.");
        return;
    }

    firstEndpoint.trigger("click");
}

function activateEndpointButton($button) {
    $("#actuator-endpoint-list .js-actuator-endpoint").removeClass("active");
    $button.addClass("active");
}

function loadActuatorEndpoint(endpointUrl, endpointPath) {
    if (!isNotEmpty(endpointUrl)) {
        return;
    }

    selectedEndpointUrl = endpointUrl;
    selectedEndpointPath = endpointPath;
    toggleRawButton(true);
    $("#viewer-endpoint-label").text(endpointPath);

    const startedAt = getNowMs();
    const requestSequence = ++latestRequestSequence;
    setViewerLoadingState(true);

    $.ajax({
        url: endpointUrl,
        method: "GET",
        dataType: "text",
        cache: false,
        global: false,
        headers: {
            "Accept": "application/json, application/openmetrics-text, text/plain, */*"
        }
    })
        .done(function (body, _textStatus, xhr) {
            processActuatorResponse(
                requestSequence,
                startedAt,
                xhr.status,
                xhr.getResponseHeader("Content-Type"),
                body,
                endpointPath,
                false
            );
        })
        .fail(function (xhr, _textStatus, errorThrown) {
            if (handleUnauthorizedApiResponse(xhr, true)) {
                return;
            }

            const statusCode = Number(xhr && xhr.status ? xhr.status : 0);
            const contentType = xhr && typeof xhr.getResponseHeader === "function"
                ? xhr.getResponseHeader("Content-Type")
                : "unknown";
            const body = isNotEmpty(xhr && xhr.responseText)
                ? xhr.responseText
                : (errorThrown || "Request failed");

            processActuatorResponse(
                requestSequence,
                startedAt,
                statusCode,
                contentType,
                body,
                endpointPath,
                true
            );
        })
        .always(function () {
            if (requestSequence === latestRequestSequence) {
                setViewerLoadingState(false);
            }
        });
}

function processActuatorResponse(requestSequence, startedAt, statusCode, contentType, body, endpointPath, isFailure) {
    if (requestSequence !== latestRequestSequence) {
        return;
    }

    const normalizedContentType = resolveContentType(contentType);
    const formatted = formatActuatorResponse(body, normalizedContentType);
    const elapsedMs = Math.max(0, Math.round(getNowMs() - startedAt));

    latestRawResponseBody = formatted.rawContent;
    latestResponseBody = formatted.prettyContent;

    renderViewerContent(formatted);
    renderViewerMeta(statusCode, normalizedContentType, elapsedMs, formatted);
    renderStructuredViewer(endpointPath, formatted);

    if (isFailure) {
        const statusSuffix = statusCode > 0 ? " (HTTP " + statusCode + ")" : "";
        notify("Error", "Failed to load " + endpointPath + statusSuffix, "error");
    }
}

function resolveContentType(contentType) {
    if (!isNotEmpty(contentType)) {
        return "unknown";
    }
    return contentType.split(";")[0].trim();
}

function formatActuatorResponse(body, contentType) {
    const textBody = normalizeRawBody(body);
    const trimmed = textBody.trim();
    const lowerContentType = (contentType || "").toLowerCase();
    const isJsonType = lowerContentType.includes("json") || looksLikeJson(trimmed);

    if (isJsonType && isNotEmpty(trimmed)) {
        try {
            const parsed = JSON.parse(trimmed);
            return {
                mode: "json",
                parsedJson: parsed,
                rawContent: textBody,
                prettyContent: JSON.stringify(parsed, null, 2),
                language: "language-json"
            };
        }
        catch (ignore) {
            // fallback to plain text rendering
        }
    }

    return {
        mode: "text",
        parsedJson: null,
        rawContent: textBody,
        prettyContent: textBody,
        language: "language-plaintext"
    };
}

function normalizeRawBody(body) {
    if (body === null || body === undefined) {
        return "";
    }
    if (typeof body === "string") {
        return body;
    }
    try {
        return JSON.stringify(body, null, 2);
    }
    catch (ignore) {
        return String(body);
    }
}

function looksLikeJson(value) {
    if (!isNotEmpty(value)) {
        return false;
    }
    return (value.startsWith("{") && value.endsWith("}"))
        || (value.startsWith("[") && value.endsWith("]"));
}

function renderViewerContent(formatted) {
    if (formatted.mode === "json" && supportsJsonViewer()) {
        renderJsonViewer(formatted.parsedJson);
        return;
    }

    $("#actuator-json-viewer").addClass("d-none").empty();
    $("#actuator-text-viewer").removeClass("d-none");

    const $response = $("#actuator-response");
    $response.removeClass("response-json response-text language-json language-plaintext");
    $response.addClass(formatted.mode === "json" ? "response-json" : "response-text");
    $response.addClass(formatted.language || "language-plaintext");
    $response.text(formatted.prettyContent || "");

    applySyntaxHighlight($response[0], formatted.prettyContent || "");
}

function renderJsonViewer(parsedJson) {
    const $jsonViewer = $("#actuator-json-viewer");

    $jsonViewer.empty();
    $jsonViewer.jsonViewer(parsedJson, {
        collapsed: false,
        rootCollapsable: true,
        withQuotes: true,
        withLinks: false
    });

    $("#actuator-text-viewer").addClass("d-none");
    $jsonViewer.removeClass("d-none");
}

function supportsJsonViewer() {
    return !!($.fn && typeof $.fn.jsonViewer === "function");
}

function renderViewerMeta(statusCode, contentType, elapsedMs, formatted) {
    const $statusBadge = $("#viewer-status-badge");
    $statusBadge
        .removeClass("badge-secondary badge-success badge-info badge-warning badge-danger")
        .addClass(resolveStatusBadgeClass(statusCode))
        .text(resolveStatusLabel(statusCode));

    const payload = formatted && isNotEmpty(formatted.rawContent) ? formatted.rawContent : "";
    const pretty = formatted && isNotEmpty(formatted.prettyContent) ? formatted.prettyContent : payload;
    const responseMode = formatted && formatted.mode === "json"
        ? (supportsJsonViewer() ? "JSON Tree" : "JSON Text")
        : "Text";

    $("#viewer-content-type").text("Content-Type: " + (contentType || "unknown"));
    $("#viewer-format").text("Format: " + responseMode);
    $("#viewer-payload-size").text("Payload: " + formatPayloadSize(payload));
    $("#viewer-line-count").text("Lines: " + countLines(pretty));
    $("#viewer-response-time").text("Response Time: " + elapsedMs + " ms");
    $("#viewer-fetched-at").text("Fetched At: " + new Date().toLocaleString());
}

function resolveStatusBadgeClass(statusCode) {
    if (statusCode >= 200 && statusCode < 300) {
        return "badge-success";
    }
    if (statusCode >= 300 && statusCode < 400) {
        return "badge-info";
    }
    if (statusCode >= 400 && statusCode < 500) {
        return "badge-warning";
    }
    if (statusCode >= 500) {
        return "badge-danger";
    }
    return "badge-secondary";
}

function resolveStatusLabel(statusCode) {
    if (statusCode > 0) {
        return "HTTP " + statusCode;
    }
    return "Request Failed";
}

function setViewerLoadingState(isLoading) {
    $("#actuator-viewer-card").toggleClass("is-loading", isLoading);
    $("#btn-actuator-refresh").prop("disabled", isLoading);
    $("#btn-actuator-copy").prop("disabled", isLoading);

    if (isLoading) {
        hideStructuredPanels();
        $("#actuator-json-viewer").addClass("d-none").empty();
        $("#actuator-text-viewer").removeClass("d-none");
        $("#actuator-response")
            .removeClass("response-json language-json")
            .addClass("response-text language-plaintext")
            .text("Loading response...");
    }
}

function toggleRawButton(enabled) {
    const $rawButton = $("#btn-actuator-open-raw");
    $rawButton.toggleClass("disabled", !enabled);
    $rawButton.attr("aria-disabled", String(!enabled));
    $rawButton.attr("href", enabled ? selectedEndpointUrl : "#");
}

function copyViewerResponse() {
    const copySource = isNotEmpty(latestResponseBody) ? latestResponseBody : latestRawResponseBody;
    if (!isNotEmpty(copySource)) {
        return;
    }

    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(copySource)
            .then(function () {
                notify("Success", "Response copied to clipboard.", "success");
            })
            .catch(function () {
                fallbackCopyToClipboard(copySource);
            });
        return;
    }

    fallbackCopyToClipboard(copySource);
}

function fallbackCopyToClipboard(text) {
    const $temp = $("<textarea>");
    $temp.css({
        position: "fixed",
        top: "0",
        left: "0",
        opacity: "0"
    });
    $("body").append($temp);
    $temp.val(text);
    $temp[0].focus();
    $temp[0].select();

    try {
        document.execCommand("copy");
        notify("Success", "Response copied to clipboard.", "success");
    }
    catch (ignore) {
        notify("Error", "Unable to copy response.", "error");
    }
    finally {
        $temp.remove();
    }
}

function renderStructuredViewer(endpointPath, formatted) {
    hideStructuredPanels();
    renderPrometheusInsight(endpointPath, formatted);
    renderProjectxInsight(endpointPath, formatted);
    renderMetricsCatalog(endpointPath, formatted);
    renderMetricInspector(endpointPath, formatted);
    renderActuatorEndpointInsight(endpointPath, formatted);
}

function hideStructuredPanels() {
    $("#viewer-insight").addClass("d-none").empty();
    $("#metrics-catalog-panel").addClass("d-none");
    $("#metric-inspector").addClass("d-none");
    $("#metric-inspector-shortcuts").empty();
}

function renderPrometheusInsight(endpointPath, formatted) {
    if (!isPrometheusEndpoint(endpointPath)) {
        return;
    }

    const payload = isNotEmpty(formatted && formatted.rawContent) ? formatted.rawContent : "";
    if (!isNotEmpty(payload)) {
        return;
    }

    const lines = payload.split(/\r\n|\r|\n/);
    let familyCount = 0;
    let seriesCount = 0;
    let commentCount = 0;

    for (let i = 0; i < lines.length; i++) {
        const line = String(lines[i] || "").trim();
        if (!line) {
            continue;
        }
        if (line.startsWith("# HELP ")) {
            familyCount += 1;
            commentCount += 1;
            continue;
        }
        if (line.startsWith("#")) {
            commentCount += 1;
            continue;
        }
        seriesCount += 1;
    }

    $("#viewer-insight")
        .removeClass("d-none")
        .html(
            "<span class='badge badge-info'>Prometheus Summary</span>"
            + "<span>Families: <strong>" + formatNumber(familyCount, 0) + "</strong></span>"
            + "<span>Series: <strong>" + formatNumber(seriesCount, 0) + "</strong></span>"
            + "<span>Comment Lines: <strong>" + formatNumber(commentCount, 0) + "</strong></span>"
        );
}

function renderProjectxInsight(endpointPath, formatted) {
    if (String(endpointPath || "") !== "/actuator/projectx" || !formatted || formatted.mode !== "json") {
        return;
    }

    const data = formatted.parsedJson || {};
    const redis = data.redis || {};
    const api = data.api || {};
    const wallet = data.wallet || {};
    const db = data.database || {};
    const overallState = firstNonBlank(data.overallState, "UNKNOWN");

    const avgApiLatency = isFiniteNumber(api.httpAverageLatencyMs) ? formatNumber(api.httpAverageLatencyMs, 2) + " ms" : "N/A";
    const redisLatency = isFiniteNumber(redis.lastLatencyMs) ? formatNumber(redis.lastLatencyMs, 2) + " ms" : "N/A";
    const walletTpm = isFiniteNumber(wallet.transactionsPerMinuteSuccess) ? formatNumber(wallet.transactionsPerMinuteSuccess, 0) : "N/A";
    const dbActive = isFiniteNumber(db.activeConnections) ? formatNumber(db.activeConnections, 0) : "N/A";
    const dbMax = isFiniteNumber(db.maxConnections) ? formatNumber(db.maxConnections, 0) : "N/A";

    $("#viewer-insight")
        .removeClass("d-none")
        .html(
            "<span class='badge badge-primary'>Projectx Summary</span>"
            + "<span>API Avg: <strong>" + avgApiLatency + "</strong></span>"
            + "<span>Redis Last: <strong>" + redisLatency + "</strong></span>"
            + "<span>Wallet TPM: <strong>" + walletTpm + "</strong></span>"
            + "<span>DB: <strong>" + dbActive + "/" + dbMax + "</strong></span>"
            + "<span>Overall: <strong>" + escapeHtml(overallState) + "</strong></span>"
        );
}

function renderMetricsCatalog(endpointPath, formatted) {
    if (!isMetricsIndexEndpoint(endpointPath) || !formatted || formatted.mode !== "json") {
        return;
    }

    const names = formatted.parsedJson && Array.isArray(formatted.parsedJson.names)
        ? formatted.parsedJson.names.slice()
        : [];
    cachedMetricCatalogNames = names.sort();
    renderMetricCatalogRows(cachedMetricCatalogNames);
    $("#metrics-catalog-panel").removeClass("d-none");

    $("#viewer-insight")
        .removeClass("d-none")
        .html(
            "<span class='badge badge-primary'>Metrics Catalog</span>"
            + "<span>Total Names: <strong>" + formatNumber(cachedMetricCatalogNames.length, 0) + "</strong></span>"
            + "<span>Tip: use filter + inspect</span>"
        );
}

function renderMetricCatalogRows(metricNames) {
    const names = Array.isArray(metricNames) ? metricNames : [];
    const maxRows = 400;
    const limited = names.slice(0, maxRows);
    const rows = [];

    for (let i = 0; i < limited.length; i++) {
        const metricName = String(limited[i] || "");
        rows.push(
            "<tr>"
            + "<td>" + (i + 1) + "</td>"
            + "<td><code>" + escapeHtml(metricName) + "</code></td>"
            + "<td><button class='btn btn-xs btn-outline-primary js-open-metric-from-catalog' data-metric-name='"
            + escapeHtml(metricName)
            + "' type='button'>Inspect</button></td>"
            + "</tr>"
        );
    }

    if (rows.length === 0) {
        rows.push(
            "<tr><td class='text-muted' colspan='3'>No metric names matched your filter.</td></tr>"
        );
    }

    if (names.length > maxRows) {
        rows.push(
            "<tr><td class='text-muted' colspan='3'>Showing first "
            + maxRows
            + " results. Narrow filter to inspect more.</td></tr>"
        );
    }

    $("#metrics-catalog-body").html(rows.join(""));
}

function applyMetricsCatalogFilter(keyword) {
    const query = String(keyword || "").trim().toLowerCase();
    if (!query) {
        renderMetricCatalogRows(cachedMetricCatalogNames);
        return;
    }
    const filtered = cachedMetricCatalogNames.filter(function (name) {
        return String(name || "").toLowerCase().includes(query);
    });
    renderMetricCatalogRows(filtered);
}

function openMetricFromCatalog(metricName) {
    if (!isNotEmpty(metricName)) {
        return;
    }
    const endpointUrl = buildActuatorEndpointUrl("/metrics/" + encodeURIComponent(metricName));
    const endpointPath = "/actuator/metrics/" + metricName;
    loadActuatorEndpoint(endpointUrl, endpointPath);
}

function openEndpointByPath(endpointPath) {
    const rawPath = String(endpointPath || "").trim();
    const normalized = normalizeEndpointPath(rawPath);
    if (!isNotEmpty(normalized)) {
        return;
    }
    const queryIndex = rawPath.indexOf("?");
    const querySuffix = queryIndex >= 0 ? rawPath.substring(queryIndex) : "";

    activateEndpointsTab();
    const $matched = $("#actuator-endpoint-list .js-actuator-endpoint").filter(function () {
        const path = normalizeEndpointPath($(this).attr("data-endpoint-path"));
        return path === normalized;
    }).first();

    if ($matched.length > 0 && !isNotEmpty(querySuffix)) {
        $matched.trigger("click");
        return;
    }

    if (normalized.startsWith("/actuator")) {
        const suffix = normalized.substring("/actuator".length);
        const endpointUrl = buildActuatorEndpointUrl(suffix) + querySuffix;
        const endpointLabel = normalized + querySuffix;
        loadActuatorEndpoint(endpointUrl, endpointLabel);
    }
}

function activateEndpointsTab() {
    const $tab = $("#actuator-endpoints-tab");
    if ($tab.length === 0) {
        return;
    }
    if (typeof $tab.tab === "function") {
        $tab.tab("show");
        return;
    }
    $tab.trigger("click");
}

function renderMetricInspector(endpointPath, formatted) {
    if (!isMetricDetailEndpoint(endpointPath) || !formatted || formatted.mode !== "json") {
        return;
    }

    const metric = formatted.parsedJson || {};
    const name = firstNonBlank(metric.name, endpointPath.replace("/actuator/metrics/", ""));
    const baseUnit = firstNonBlank(metric.baseUnit, "-");
    const description = firstNonBlank(metric.description, "");
    const measurements = Array.isArray(metric.measurements) ? metric.measurements : [];
    const tags = Array.isArray(metric.availableTags) ? metric.availableTags : [];

    $("#metric-inspector-name").text(name);
    $("#metric-inspector-base-unit").text("Base Unit: " + baseUnit + (description ? " | " + description : ""));

    const measurementRows = [];
    if (measurements.length === 0) {
        measurementRows.push("<tr><td class='text-muted' colspan='2'>No measurements returned.</td></tr>");
    }
    else {
        for (let i = 0; i < measurements.length; i++) {
            const item = measurements[i] || {};
            measurementRows.push(
                "<tr>"
                + "<td><code>" + escapeHtml(String(item.statistic || "-")) + "</code></td>"
                + "<td class='text-right'><strong>" + escapeHtml(formatNumber(item.value, 6)) + "</strong></td>"
                + "</tr>"
            );
        }
    }
    $("#metric-inspector-measurements").html(measurementRows.join(""));

    const tagParts = [];
    if (tags.length === 0) {
        tagParts.push("<span class='text-muted'>No available tags for this metric.</span>");
    }
    else {
        for (let i = 0; i < tags.length; i++) {
            const tag = tags[i] || {};
            const tagName = String(tag.tag || "");
            const values = Array.isArray(tag.values) ? tag.values : [];
            const preview = values.slice(0, 4).join(", ");
            tagParts.push(
                "<span class='metric-tag-chip'>"
                + escapeHtml(tagName)
                + ": "
                + escapeHtml(preview + (values.length > 4 ? "..." : ""))
                + "</span>"
            );
        }
    }
    $("#metric-inspector-tags").html(tagParts.join(""));
    renderMetricShortcuts(name, metric);
    $("#metric-inspector").removeClass("d-none");

    if (isHikariCompatibilityMetric(name)) {
        renderHikariMetricBreakdown(name, metric);
    }
}

function renderMetricShortcuts(metricName, metric) {
    const normalizedName = String(metricName || "");
    if (!isNotEmpty(normalizedName)) {
        $("#metric-inspector-shortcuts").empty();
        return;
    }
    const buttons = [];

    buttons.push(buildMetricShortcutButton("/actuator/metrics/" + normalizedName, "Reload"));

    if (isHikariCompatibilityMetric(normalizedName)) {
        const metricRoot = normalizedName === "hikaaricp" ? "hikaaricp" : "hikaricp";
        buttons.push(buildMetricShortcutButton("/actuator/metrics/" + metricRoot + "/summary", "Pool Summary"));
        buttons.push(buildMetricShortcutButton("/actuator/metrics/" + metricRoot + "?tag=component:active", "Active"));
        buttons.push(buildMetricShortcutButton("/actuator/metrics/" + metricRoot + "?tag=component:pending", "Pending"));
    }
    else if (normalizedName === "http.server.requests") {
        buttons.push(buildMetricShortcutButton("/actuator/metrics/http.server.requests?tag=outcome:SUCCESS", "Success"));
        buttons.push(buildMetricShortcutButton("/actuator/metrics/http.server.requests?tag=outcome:SERVER_ERROR", "Server Errors"));
        buttons.push(buildMetricShortcutButton("/actuator/metrics/http.server.requests?tag=outcome:CLIENT_ERROR", "Client Errors"));
    }
    else if (normalizedName === "redis.command") {
        buttons.push(buildMetricShortcutButton("/actuator/metrics/redis.command?tag=result:success", "Success"));
        buttons.push(buildMetricShortcutButton("/actuator/metrics/redis.command?tag=result:failure", "Failure"));
    }
    else if (normalizedName === "projectx.wallet.transactions.per_minute") {
        buttons.push(buildMetricShortcutButton("/actuator/metrics/projectx.wallet.transactions.per_minute?tag=status:success", "Success"));
        buttons.push(buildMetricShortcutButton("/actuator/metrics/projectx.wallet.transactions.per_minute?tag=status:failed", "Failed"));
    }

    const availableTags = Array.isArray(metric && metric.availableTags) ? metric.availableTags : [];
    for (let i = 0; i < availableTags.length; i++) {
        const tag = availableTags[i] || {};
        const tagName = String(tag.tag || "");
        const values = Array.isArray(tag.values) ? tag.values.slice(0, 2) : [];
        for (let j = 0; j < values.length; j++) {
            buttons.push(buildMetricShortcutButton(
                "/actuator/metrics/" + normalizedName + "?tag=" + encodeURIComponent(tagName + ":" + values[j]),
                tagName + ": " + values[j]
            ));
        }
    }

    $("#metric-inspector-shortcuts").html(buttons.join(""));
}

function buildMetricShortcutButton(endpointPath, label) {
    if (!isNotEmpty(endpointPath)) {
        return "";
    }
    return "<button class='btn btn-xs btn-outline-info js-open-endpoint' data-endpoint-path='"
        + escapeHtml(endpointPath)
        + "' type='button'>"
        + escapeHtml(label)
        + "</button>";
}

function renderHikariMetricBreakdown(metricName, metric) {
    const poolValues = getMetricTagValues(metric, "pool");
    const componentValues = sortHikariComponents(getMetricTagValues(metric, "component"));
    if (componentValues.length === 0) {
        return;
    }
    const metricRoot = String(metricName || "").trim() === "hikaaricp" ? "hikaaricp" : "hikaricp";

    const pools = poolValues.length > 0 ? poolValues : [""];
    const targets = [];
    for (let i = 0; i < pools.length; i++) {
        for (let j = 0; j < componentValues.length; j++) {
            targets.push({
                pool: pools[i],
                component: componentValues[j]
            });
        }
    }

    const loadingRows = [];
    for (let i = 0; i < targets.length; i++) {
        const target = targets[i];
        const label = buildHikariBreakdownLabel(target.pool, target.component);
        loadingRows.push(
            "<tr>"
            + "<td><code>" + escapeHtml(label) + "</code></td>"
            + "<td class='text-right text-muted'>Loading...</td>"
            + "</tr>"
        );
    }
    $("#metric-inspector-measurements").html(loadingRows.join(""));

    const requests = targets.map(function (target) {
        return fetchMetricSafely(metricRoot, buildHikariMetricTags(target.pool, target.component));
    });
    if (requests.length === 0) {
        return;
    }

    $.when.apply($, requests).done(function () {
        if (!isHikariMetricEndpointSelected()) {
            return;
        }

        const responses = normalizeDeferredValues(arguments, requests.length);
        const rows = [];
        for (let i = 0; i < targets.length; i++) {
            const target = targets[i];
            const response = responses[i];
            const value = readMeasurement(response, ["VALUE"]);
            const label = buildHikariBreakdownLabel(target.pool, target.component);
            rows.push(
                "<tr>"
                + "<td><code>" + escapeHtml(label) + "</code></td>"
                + "<td class='text-right'><strong>" + escapeHtml(formatNumber(value, 4)) + "</strong></td>"
                + "</tr>"
            );
        }
        $("#metric-inspector-measurements").html(rows.join(""));

        const firstPool = pools.length > 0 ? pools[0] : "";
        const firstComponent = componentValues.length > 0 ? componentValues[0] : "active";
        const queryParts = [];
        if (isNotEmpty(firstPool)) {
            queryParts.push("tag=pool:" + firstPool);
        }
        queryParts.push("tag=component:" + firstComponent);
        const sampleQuery = "/actuator/metrics/" + metricRoot + "?" + queryParts.join("&");
        $("#metric-inspector-tags").append(
            "<span class='metric-tag-chip'>sample query: " + escapeHtml(sampleQuery) + "</span>"
        );
    });
}

function getMetricTagValues(metric, tagName) {
    const tags = Array.isArray(metric && metric.availableTags) ? metric.availableTags : [];
    const match = tags.find(function (item) {
        return String(item && item.tag || "") === String(tagName || "");
    });
    const values = match && Array.isArray(match.values) ? match.values : [];
    return values.filter(function (value) {
        return isNotEmpty(value);
    });
}

function sortHikariComponents(values) {
    const order = {
        active: 1,
        idle: 2,
        pending: 3,
        total: 4,
        max: 5,
        min: 6,
        utilization_percent: 7
    };
    return (Array.isArray(values) ? values.slice() : []).sort(function (left, right) {
        const leftKey = String(left || "");
        const rightKey = String(right || "");
        const leftOrder = order.hasOwnProperty(leftKey) ? order[leftKey] : 999;
        const rightOrder = order.hasOwnProperty(rightKey) ? order[rightKey] : 999;
        if (leftOrder !== rightOrder) {
            return leftOrder - rightOrder;
        }
        return leftKey.localeCompare(rightKey);
    });
}

function buildHikariMetricTags(pool, component) {
    const tags = {
        component: component
    };
    if (isNotEmpty(pool)) {
        tags.pool = pool;
    }
    return tags;
}

function buildHikariBreakdownLabel(pool, component) {
    const componentLabel = String(component || "unknown");
    if (!isNotEmpty(pool)) {
        return componentLabel;
    }
    return componentLabel + " [" + pool + "]";
}

function normalizeDeferredValues(argsObject, expectedCount) {
    if (!expectedCount || expectedCount <= 0) {
        return [];
    }
    if (expectedCount === 1) {
        return [argsObject[0]];
    }
    return Array.prototype.slice.call(argsObject);
}

function renderActuatorEndpointInsight(endpointPath, formatted) {
    if (!formatted || formatted.mode !== "json") {
        return;
    }

    if (!$("#viewer-insight").hasClass("d-none")) {
        return;
    }

    const path = normalizeEndpointPath(endpointPath);
    const payload = formatted.parsedJson || {};
    let insightHtml = "";

    if (path === "/actuator" || path === "/actuator/") {
        insightHtml = buildActuatorIndexInsight(payload);
    }
    else if (path === "/actuator/health" || path.startsWith("/actuator/health/")) {
        insightHtml = buildHealthInsight(payload);
    }
    else if (path === "/actuator/info") {
        insightHtml = buildInfoInsight(payload);
    }
    else if (path === "/actuator/threaddump") {
        insightHtml = buildThreadDumpInsight(payload);
    }
    else if (path === "/actuator/loggers") {
        insightHtml = buildLoggersInsight(payload);
    }
    else if (path === "/actuator/caches") {
        insightHtml = buildCachesInsight(payload);
    }
    else if (path === "/actuator/scheduledtasks") {
        insightHtml = buildScheduledTasksInsight(payload);
    }
    else if (path === "/actuator/mappings") {
        insightHtml = buildMappingsInsight(payload);
    }
    else if (path === "/actuator/beans") {
        insightHtml = buildBeansInsight(payload);
    }
    else if (path === "/actuator/conditions") {
        insightHtml = buildConditionsInsight(payload);
    }
    else if (path.startsWith("/actuator/configprops")) {
        insightHtml = buildConfigPropsInsight(payload);
    }
    else if (path.startsWith("/actuator/env")) {
        insightHtml = buildEnvInsight(payload);
    }
    else if (
        path === "/actuator/metrics/hikaricp"
        || path === "/actuator/metrics/hikaricp/summary"
        || path === "/actuator/metrics/hikaaricp"
        || path === "/actuator/metrics/hikaaricp/summary"
    ) {
        insightHtml = buildHikariSummaryInsight(payload);
    }

    if (isNotEmpty(insightHtml)) {
        $("#viewer-insight").removeClass("d-none").html(insightHtml);
    }
}

function buildActuatorIndexInsight(payload) {
    const links = payload && payload._links ? payload._links : {};
    const endpointCount = Object.keys(links).length;
    return "<span class='badge badge-primary'>Actuator Index</span>"
        + "<span>Exposed Links: <strong>" + formatNumber(endpointCount, 0) + "</strong></span>";
}

function buildHealthInsight(payload) {
    const status = firstNonBlank(payload && payload.status, "UNKNOWN");
    const components = payload && payload.components ? payload.components : {};
    const componentNames = Object.keys(components);
    let downCount = 0;

    for (let i = 0; i < componentNames.length; i++) {
        const component = components[componentNames[i]] || {};
        const componentStatus = String(component.status || "").toUpperCase();
        if (componentStatus && componentStatus !== "UP") {
            downCount += 1;
        }
    }

    const badgeClass = String(status).toUpperCase() === "UP" ? "badge-success" : "badge-warning";
    return "<span class='badge " + badgeClass + "'>Health</span>"
        + "<span>Status: <strong>" + escapeHtml(String(status)) + "</strong></span>"
        + "<span>Components: <strong>" + formatNumber(componentNames.length, 0) + "</strong></span>"
        + "<span>Degraded: <strong>" + formatNumber(downCount, 0) + "</strong></span>";
}

function buildInfoInsight(payload) {
    const app = payload && payload.app ? payload.app : {};
    const runtime = payload && payload.runtime ? payload.runtime : {};
    const activeProfiles = Array.isArray(app.activeProfiles) ? app.activeProfiles.length : 0;
    const topLevelKeys = Object.keys(payload || {}).length;

    return "<span class='badge badge-info'>Application Info</span>"
        + "<span>App: <strong>" + escapeHtml(firstNonBlank(app.name, "-")) + "</strong></span>"
        + "<span>Profiles: <strong>" + formatNumber(activeProfiles, 0) + "</strong></span>"
        + "<span>Java: <strong>" + escapeHtml(firstNonBlank(runtime.javaVersion, "-")) + "</strong></span>"
        + "<span>Sections: <strong>" + formatNumber(topLevelKeys, 0) + "</strong></span>";
}

function buildThreadDumpInsight(payload) {
    const threads = Array.isArray(payload && payload.threads) ? payload.threads : [];
    let runnable = 0;
    let blocked = 0;
    let waiting = 0;

    for (let i = 0; i < threads.length; i++) {
        const thread = threads[i] || {};
        const state = String(thread.threadState || "").toUpperCase();
        if (state === "RUNNABLE") {
            runnable += 1;
        }
        else if (state === "BLOCKED") {
            blocked += 1;
        }
        else if (state.includes("WAITING")) {
            waiting += 1;
        }
    }

    return "<span class='badge badge-dark'>Thread Dump</span>"
        + "<span>Total Threads: <strong>" + formatNumber(threads.length, 0) + "</strong></span>"
        + "<span>RUNNABLE: <strong>" + formatNumber(runnable, 0) + "</strong></span>"
        + "<span>BLOCKED: <strong>" + formatNumber(blocked, 0) + "</strong></span>"
        + "<span>WAITING: <strong>" + formatNumber(waiting, 0) + "</strong></span>";
}

function buildLoggersInsight(payload) {
    const loggers = payload && payload.loggers ? payload.loggers : {};
    const loggerNames = Object.keys(loggers);
    let debugCount = 0;

    for (let i = 0; i < loggerNames.length; i++) {
        const logger = loggers[loggerNames[i]] || {};
        const configuredLevel = String(logger.configuredLevel || "").toUpperCase();
        if (configuredLevel === "DEBUG" || configuredLevel === "TRACE") {
            debugCount += 1;
        }
    }

    return "<span class='badge badge-secondary'>Loggers</span>"
        + "<span>Total: <strong>" + formatNumber(loggerNames.length, 0) + "</strong></span>"
        + "<span>DEBUG/TRACE: <strong>" + formatNumber(debugCount, 0) + "</strong></span>";
}

function buildCachesInsight(payload) {
    const cacheManagers = payload && payload.cacheManagers ? payload.cacheManagers : {};
    const managerNames = Object.keys(cacheManagers);
    let cacheCount = 0;

    for (let i = 0; i < managerNames.length; i++) {
        const manager = cacheManagers[managerNames[i]] || {};
        const caches = manager.caches || {};
        cacheCount += Object.keys(caches).length;
    }

    return "<span class='badge badge-warning'>Caches</span>"
        + "<span>Managers: <strong>" + formatNumber(managerNames.length, 0) + "</strong></span>"
        + "<span>Caches: <strong>" + formatNumber(cacheCount, 0) + "</strong></span>";
}

function buildScheduledTasksInsight(payload) {
    const cronTasks = Array.isArray(payload && payload.cron) ? payload.cron : [];
    const fixedDelayTasks = Array.isArray(payload && payload.fixedDelay) ? payload.fixedDelay : [];
    const fixedRateTasks = Array.isArray(payload && payload.fixedRate) ? payload.fixedRate : [];
    const customTasks = Array.isArray(payload && payload.custom) ? payload.custom : [];

    const total = cronTasks.length + fixedDelayTasks.length + fixedRateTasks.length + customTasks.length;

    return "<span class='badge badge-primary'>Scheduled Tasks</span>"
        + "<span>Total: <strong>" + formatNumber(total, 0) + "</strong></span>"
        + "<span>Cron: <strong>" + formatNumber(cronTasks.length, 0) + "</strong></span>"
        + "<span>Fixed Delay: <strong>" + formatNumber(fixedDelayTasks.length, 0) + "</strong></span>"
        + "<span>Fixed Rate: <strong>" + formatNumber(fixedRateTasks.length, 0) + "</strong></span>";
}

function buildMappingsInsight(payload) {
    const contexts = payload && payload.contexts ? payload.contexts : {};
    const contextNames = Object.keys(contexts);
    let mappingCount = 0;

    for (let i = 0; i < contextNames.length; i++) {
        const context = contexts[contextNames[i]] || {};
        const mappings = context.mappings || {};
        const servletMappings = mappings.dispatcherServlets || {};
        const servletNames = Object.keys(servletMappings);

        for (let j = 0; j < servletNames.length; j++) {
            const rows = Array.isArray(servletMappings[servletNames[j]]) ? servletMappings[servletNames[j]] : [];
            mappingCount += rows.length;
        }
    }

    return "<span class='badge badge-dark'>Mappings</span>"
        + "<span>Contexts: <strong>" + formatNumber(contextNames.length, 0) + "</strong></span>"
        + "<span>Handler Mappings: <strong>" + formatNumber(mappingCount, 0) + "</strong></span>";
}

function buildBeansInsight(payload) {
    const contexts = payload && payload.contexts ? payload.contexts : {};
    const contextNames = Object.keys(contexts);
    let beanCount = 0;

    for (let i = 0; i < contextNames.length; i++) {
        const context = contexts[contextNames[i]] || {};
        const beans = context.beans || {};
        beanCount += Object.keys(beans).length;
    }

    return "<span class='badge badge-secondary'>Beans</span>"
        + "<span>Contexts: <strong>" + formatNumber(contextNames.length, 0) + "</strong></span>"
        + "<span>Total Beans: <strong>" + formatNumber(beanCount, 0) + "</strong></span>";
}

function buildConditionsInsight(payload) {
    const contexts = payload && payload.contexts ? payload.contexts : {};
    const contextNames = Object.keys(contexts);
    let positives = 0;
    let negatives = 0;
    let unconditional = 0;

    for (let i = 0; i < contextNames.length; i++) {
        const context = contexts[contextNames[i]] || {};
        const positiveMatches = context.positiveMatches || {};
        const negativeMatches = context.negativeMatches || {};
        const unconditionalClasses = Array.isArray(context.unconditionalClasses) ? context.unconditionalClasses : [];

        positives += Object.keys(positiveMatches).length;
        negatives += Object.keys(negativeMatches).length;
        unconditional += unconditionalClasses.length;
    }

    return "<span class='badge badge-warning'>Conditions</span>"
        + "<span>Positive: <strong>" + formatNumber(positives, 0) + "</strong></span>"
        + "<span>Negative: <strong>" + formatNumber(negatives, 0) + "</strong></span>"
        + "<span>Unconditional: <strong>" + formatNumber(unconditional, 0) + "</strong></span>";
}

function buildConfigPropsInsight(payload) {
    const contexts = payload && payload.contexts ? payload.contexts : {};
    const contextNames = Object.keys(contexts);
    let beanCount = 0;

    for (let i = 0; i < contextNames.length; i++) {
        const context = contexts[contextNames[i]] || {};
        const beans = context.beans || {};
        beanCount += Object.keys(beans).length;
    }

    return "<span class='badge badge-info'>Config Props</span>"
        + "<span>Contexts: <strong>" + formatNumber(contextNames.length, 0) + "</strong></span>"
        + "<span>Config Beans: <strong>" + formatNumber(beanCount, 0) + "</strong></span>";
}

function buildEnvInsight(payload) {
    const propertySources = Array.isArray(payload && payload.propertySources) ? payload.propertySources : [];
    const activeProfiles = Array.isArray(payload && payload.activeProfiles) ? payload.activeProfiles : [];

    return "<span class='badge badge-primary'>Environment</span>"
        + "<span>Active Profiles: <strong>" + formatNumber(activeProfiles.length, 0) + "</strong></span>"
        + "<span>Property Sources: <strong>" + formatNumber(propertySources.length, 0) + "</strong></span>";
}

function buildHikariSummaryInsight(payload) {
    if (payload && payload.totals) {
        const totals = payload.totals || {};
        const pools = Array.isArray(payload.pools) ? payload.pools : [];
        const active = Number(totals.activeConnections || 0);
        const max = Number(totals.maxConnections || 0);
        const utilization = Number(totals.utilizationPercent || 0);

        return "<span class='badge badge-success'>HikariCP</span>"
            + "<span>Pools: <strong>" + formatNumber(pools.length, 0) + "</strong></span>"
            + "<span>Active: <strong>" + formatNumber(active, 0) + "</strong></span>"
            + "<span>Max: <strong>" + formatNumber(max, 0) + "</strong></span>"
            + "<span>Utilization: <strong>" + formatNumber(utilization, 1) + "%</strong></span>";
    }

    const availableTags = Array.isArray(payload && payload.availableTags) ? payload.availableTags : [];
    const componentTag = availableTags.find(function (tag) {
        return String(tag && tag.tag || "") === "component";
    });
    const poolTag = availableTags.find(function (tag) {
        return String(tag && tag.tag || "") === "pool";
    });
    const componentCount = componentTag && Array.isArray(componentTag.values)
        ? componentTag.values.length
        : 0;
    const poolCount = poolTag && Array.isArray(poolTag.values)
        ? poolTag.values.length
        : 0;
    const metricValue = readMeasurement(payload, ["VALUE", "COUNT", "TOTAL_TIME", "MAX"]);

    return "<span class='badge badge-success'>HikariCP Metric</span>"
        + "<span>Pools: <strong>" + formatNumber(poolCount, 0) + "</strong></span>"
        + "<span>Components: <strong>" + formatNumber(componentCount, 0) + "</strong></span>"
        + "<span>Current Value: <strong>" + formatNumber(metricValue, 2) + "</strong></span>";
}

function normalizeEndpointPath(endpointPath) {
    const normalized = String(endpointPath || "").trim();
    if (!normalized) {
        return "";
    }
    const qIndex = normalized.indexOf("?");
    return qIndex >= 0 ? normalized.substring(0, qIndex) : normalized;
}

function isPrometheusEndpoint(endpointPath) {
    return String(endpointPath || "").includes("/actuator/prometheus");
}

function isMetricsIndexEndpoint(endpointPath) {
    return String(endpointPath || "").endsWith("/actuator/metrics");
}

function isMetricDetailEndpoint(endpointPath) {
    const normalized = normalizeEndpointPath(endpointPath);
    if (!normalized || normalized === "/actuator/metrics") {
        return false;
    }
    if (
        normalized === "/actuator/metrics/hikaricp"
        || normalized === "/actuator/metrics/hikaricp/summary"
        || normalized === "/actuator/metrics/hikaaricp"
        || normalized === "/actuator/metrics/hikaaricp/summary"
    ) {
        return false;
    }
    if (!normalized.startsWith("/actuator/metrics/")) {
        return false;
    }
    if (normalized.endsWith("/summary")) {
        return false;
    }
    return true;
}

function escapeHtml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function formatRedisKeyForDisplay(key) {
    const fullKey = String(key || "");
    const lastColonIndex = fullKey.lastIndexOf(":");

    if (lastColonIndex <= 0 || lastColonIndex >= fullKey.length - 1) {
        return fullKey;
    }

    const suffix = fullKey.substring(lastColonIndex + 1);
    if (!shouldTrimRedisKeySuffix(suffix)) {
        return fullKey;
    }

    return fullKey.substring(0, lastColonIndex);
}

function shouldTrimRedisKeySuffix(suffix) {
    if (!isNotEmpty(suffix)) {
        return false;
    }

    const token = String(suffix || "").trim();
    if (token.length < 24) {
        return false;
    }
    if (/^[a-fA-F0-9]{24,}$/.test(token)) {
        return true;
    }
    return token.length >= 32 && /^[A-Za-z0-9_-]{32,}$/.test(token);
}

function isHikariCompatibilityMetric(metricName) {
    const normalized = String(metricName || "").trim();
    return normalized === "hikaricp" || normalized === "hikaaricp";
}

function isHikariMetricEndpointSelected() {
    const selectedPath = normalizeEndpointPath(selectedEndpointPath);
    return selectedPath === "/actuator/metrics/hikaricp"
        || selectedPath === "/actuator/metrics/hikaaricp";
}

function refreshOperationalSnapshot(_isManualRefresh) {
    const $refreshButton = $(SNAPSHOT_REFRESH_BUTTON_SELECTOR);
    if ($refreshButton.length === 0 || $refreshButton.prop("disabled")) {
        return;
    }

    $refreshButton.prop("disabled", true);

    fetchProjectxSnapshot()
        .done(function (projectxResponse) {
            if (isProjectxSnapshotPayload(projectxResponse)) {
                renderOperationalSnapshotFromProjectx(projectxResponse);
                $("#snapshot-fetched-at").text("Last snapshot: " + new Date().toLocaleString());
                $refreshButton.prop("disabled", false);
                return;
            }
            refreshOperationalSnapshotFromMetricEndpoints($refreshButton);
        })
        .fail(function () {
            refreshOperationalSnapshotFromMetricEndpoints($refreshButton);
        });
}

function fetchProjectxSnapshot() {
    return $.ajax({
        url: buildActuatorEndpointUrl("/projectx"),
        method: "GET",
        dataType: "json",
        cache: false,
        global: false
    });
}

function isProjectxSnapshotPayload(payload) {
    return !!(payload && typeof payload === "object"
        && (payload.wallet || payload.api || payload.database || payload.redis));
}

function renderOperationalSnapshotFromProjectx(payload) {
    const wallet = payload && payload.wallet ? payload.wallet : {};
    const api = payload && payload.api ? payload.api : {};
    const database = payload && payload.database ? payload.database : {};
    const redis = payload && payload.redis ? payload.redis : {};
    const thresholds = payload && payload.thresholds ? payload.thresholds : {};

    setSnapshotState("#snapshot-wallet-state", firstNonBlank(wallet.state, "UNKNOWN"));
    setSnapshotState("#snapshot-api-state", firstNonBlank(api.state, "UNKNOWN"));
    setSnapshotState("#snapshot-db-state", firstNonBlank(database.state, "UNKNOWN"));
    setSnapshotState("#snapshot-redis-state", firstNonBlank(redis.state, "UNKNOWN"));
    setOverallSnapshotState(firstNonBlank(payload && payload.overallState, "UNKNOWN"));
    applySnapshotThresholds(thresholds);

    const walletSuccess = Number(wallet.transactionsPerMinuteSuccess);
    const walletFailed = Number(wallet.transactionsPerMinuteFailed);
    const walletTotal = Number(wallet.transactionsPerMinuteTotal);
    if (isFiniteNumber(walletTotal) || isFiniteNumber(walletSuccess) || isFiniteNumber(walletFailed)) {
        const safeSuccess = isFiniteNumber(walletSuccess) ? walletSuccess : 0;
        const safeFailed = isFiniteNumber(walletFailed) ? walletFailed : 0;
        const safeTotal = isFiniteNumber(walletTotal) ? walletTotal : (safeSuccess + safeFailed);
        pushTrendPoint("wallet", safeTotal);
        renderTrend("wallet", "#snapshot-wallet-trend");
        setSnapshotValue(
            "#snapshot-wallet-tpm",
            formatNumber(safeTotal, 0) + " tx/min",
            "Success " + formatNumber(safeSuccess, 0) + " / Failed " + formatNumber(safeFailed, 0)
        );
    }
    else {
        renderTrend("wallet", "#snapshot-wallet-trend");
        setSnapshotValue("#snapshot-wallet-tpm", "N/A", "projectx.wallet.transactions.per_minute unavailable");
    }

    const apiAverageLatencyMs = Number(api.httpAverageLatencyMs);
    if (isFiniteNumber(apiAverageLatencyMs)) {
        const apiRequestCount = Number(api.httpRequestCount);
        const apiMaxLatencyMs = Number(api.httpMaxLatencyMs);
        const apiSubParts = [];
        if (isFiniteNumber(apiRequestCount)) {
            apiSubParts.push("count " + formatNumber(apiRequestCount, 0));
        }
        if (isFiniteNumber(apiMaxLatencyMs)) {
            apiSubParts.push("max " + formatNumber(apiMaxLatencyMs, 2) + " ms");
        }
        pushTrendPoint("apiLatency", apiAverageLatencyMs);
        renderTrend("apiLatency", "#snapshot-api-trend");
        setSnapshotValue(
            "#snapshot-api-latency",
            formatNumber(apiAverageLatencyMs, 2) + " ms",
            apiSubParts.length > 0 ? apiSubParts.join(", ") : "from /actuator/projectx"
        );
    }
    else {
        renderTrend("apiLatency", "#snapshot-api-trend");
        setSnapshotValue("#snapshot-api-latency", "N/A", "http.server.requests unavailable");
    }

    const dbActiveConnections = Number(database.activeConnections);
    const dbMaxConnections = Number(database.maxConnections);
    const dbUtilizationPercent = Number(database.utilizationPercent);
    if (isFiniteNumber(dbActiveConnections) && isFiniteNumber(dbMaxConnections)) {
        const dbState = firstNonBlank(database.state, "UNKNOWN");
        const safeDbUtilization = isFiniteNumber(dbUtilizationPercent) ? dbUtilizationPercent : 0;
        pushTrendPoint("dbUtilization", safeDbUtilization);
        renderTrend("dbUtilization", "#snapshot-db-trend");
        const dbSubText = isFiniteNumber(dbUtilizationPercent)
            ? formatNumber(dbUtilizationPercent, 1) + "% active (" + dbState + ")"
            : "state " + dbState;
        setSnapshotValue(
            "#snapshot-db-pool",
            formatNumber(dbActiveConnections, 0) + "/" + formatNumber(dbMaxConnections, 0),
            dbSubText
        );
    }
    else {
        renderTrend("dbUtilization", "#snapshot-db-trend");
        setSnapshotValue("#snapshot-db-pool", "N/A", "hikaricp.connections unavailable");
    }

    const redisLastLatencyMs = Number(redis.lastLatencyMs);
    const redisCommandCount = Number(redis.commandCount);
    const redisFailureRatePercent = Number(redis.failureRatePercent);
    const redisConsecutiveFailures = Number(redis.consecutiveFailures);
    if (isFiniteNumber(redisLastLatencyMs)) {
        const redisSubParts = [];
        if (isFiniteNumber(redisCommandCount)) {
            redisSubParts.push("count " + formatNumber(redisCommandCount, 0));
        }
        if (isFiniteNumber(redisFailureRatePercent)) {
            redisSubParts.push("fail " + formatNumber(redisFailureRatePercent, 1) + "%");
        }
        if (isFiniteNumber(redisConsecutiveFailures) && redisConsecutiveFailures > 0) {
            redisSubParts.push("consecutive fail " + formatNumber(redisConsecutiveFailures, 0));
        }
        pushTrendPoint("redisLatency", redisLastLatencyMs);
        renderTrend("redisLatency", "#snapshot-redis-trend");
        setSnapshotValue(
            "#snapshot-redis-latency",
            formatNumber(redisLastLatencyMs, 2) + " ms",
            redisSubParts.length > 0 ? redisSubParts.join(", ") : "redis.command"
        );
    }
    else if (isFiniteNumber(redisCommandCount) && redisCommandCount === 0) {
        pushTrendPoint("redisLatency", 0);
        renderTrend("redisLatency", "#snapshot-redis-trend");
        setSnapshotValue("#snapshot-redis-latency", "0.00 ms", "redis.command count 0");
    }
    else {
        renderTrend("redisLatency", "#snapshot-redis-trend");
        setSnapshotValue("#snapshot-redis-latency", "N/A", "redis.command unavailable");
    }
}

function refreshOperationalSnapshotFromMetricEndpoints($refreshButton) {
    fetchMetricIndex()
        .done(function (metricIndexResponse) {
            const metricNames = Array.isArray(metricIndexResponse && metricIndexResponse.names)
                ? metricIndexResponse.names
                : [];

            const httpMetricName = resolveFirstMetricName(metricNames, [
                "http.server.requests",
                "http.client.requests"
            ]);
            const dbActiveMetricName = resolveFirstMetricName(metricNames, [
                "hikaricp.connections.active",
                "jdbc.connections.active"
            ]);
            const dbMaxMetricName = resolveFirstMetricName(metricNames, [
                "hikaricp.connections.max",
                "jdbc.connections.max"
            ]);
            const redisMetricName = resolveFirstMetricName(metricNames, [
                "redis.command",
                "redis.commands",
                "cache.gets"
            ]);

            $.when(
                fetchMetricSafely("projectx.wallet.transactions.per_minute", {"status": "success"}),
                fetchMetricSafely("projectx.wallet.transactions.per_minute", {"status": "failed"}),
                fetchMetricSafely(httpMetricName),
                fetchMetricSafely(dbActiveMetricName),
                fetchMetricSafely(dbMaxMetricName),
                fetchMetricSafely(redisMetricName),
                fetchMetricSafely("cache.gets")
            )
                .done(function (walletSuccess, walletFailed, httpServer, dbActive, dbMax, redisMetric, cacheGets) {
                    renderWalletSnapshot(walletSuccess, walletFailed);
                    renderApiLatencySnapshot(httpServer, httpMetricName);
                    renderDbPoolSnapshot(dbActive, dbMax, dbActiveMetricName, dbMaxMetricName);
                    renderRedisSnapshot(redisMetric, cacheGets, redisMetricName);
                    applySnapshotThresholds(fallbackThresholdPayload());
                    refreshOverallSnapshotStateFromTiles();
                    $("#snapshot-fetched-at").text("Last snapshot: " + new Date().toLocaleString());
                })
                .always(function () {
                    $refreshButton.prop("disabled", false);
                });
        })
        .fail(function () {
            setSnapshotState("#snapshot-wallet-state", "UNKNOWN");
            setSnapshotState("#snapshot-api-state", "UNKNOWN");
            setSnapshotState("#snapshot-db-state", "UNKNOWN");
            setSnapshotState("#snapshot-redis-state", "UNKNOWN");
            setOverallSnapshotState("UNKNOWN");
            applySnapshotThresholds(fallbackThresholdPayload());
            setSnapshotValue("#snapshot-wallet-tpm", "N/A", "Actuator metrics index unavailable");
            setSnapshotValue("#snapshot-api-latency", "N/A", "Actuator metrics index unavailable");
            setSnapshotValue("#snapshot-db-pool", "N/A", "Actuator metrics index unavailable");
            setSnapshotValue("#snapshot-redis-latency", "N/A", "Actuator metrics index unavailable");
            $refreshButton.prop("disabled", false);
        });
}

function fetchMetricSafely(metricName, tags) {
    const deferred = $.Deferred();
    if (!isNotEmpty(metricName)) {
        deferred.resolve(null);
        return deferred.promise();
    }

    fetchActuatorMetric(metricName, tags)
        .done(function (response) {
            deferred.resolve(response);
        })
        .fail(function () {
            deferred.resolve(null);
        });
    return deferred.promise();
}

function fetchActuatorMetric(metricName, tags) {
    return $.ajax({
        url: buildMetricEndpointUrl(metricName, tags),
        method: "GET",
        dataType: "json",
        cache: false,
        global: false
    });
}

function fetchMetricIndex() {
    return $.ajax({
        url: buildActuatorEndpointUrl("/metrics"),
        method: "GET",
        dataType: "json",
        cache: false,
        global: false
    });
}

function buildMetricEndpointUrl(metricName, tags) {
    let endpoint = buildActuatorEndpointUrl("/metrics/" + encodeURIComponent(metricName || ""));
    const queryPairs = [];

    if (tags && typeof tags === "object") {
        Object.keys(tags).forEach(function (tagName) {
            const tagValue = tags[tagName];
            if (isNotEmpty(tagName) && isNotEmpty(tagValue)) {
                queryPairs.push("tag=" + encodeURIComponent(tagName + ":" + tagValue));
            }
        });
    }

    if (queryPairs.length > 0) {
        endpoint += "?" + queryPairs.join("&");
    }

    return endpoint;
}

function renderWalletSnapshot(walletSuccessMetric, walletFailedMetric) {
    const successPerMinute = readMeasurement(walletSuccessMetric, ["VALUE"]);
    const failedPerMinute = readMeasurement(walletFailedMetric, ["VALUE"]);

    if (successPerMinute === null && failedPerMinute === null) {
        setSnapshotState("#snapshot-wallet-state", "UNKNOWN");
        setOverallSnapshotState("UNKNOWN");
        applySnapshotThresholds(fallbackThresholdPayload());
        renderTrend("wallet", "#snapshot-wallet-trend");
        setSnapshotValue("#snapshot-wallet-tpm", "N/A", "projectx.wallet.transactions.per_minute unavailable");
        return;
    }

    const successValue = successPerMinute === null ? 0 : successPerMinute;
    const failedValue = failedPerMinute === null ? 0 : failedPerMinute;
    const totalValue = successValue + failedValue;
    const failedRatePercent = totalValue <= 0 ? 0 : (failedValue * 100.0 / totalValue);
    const walletState = resolveStateByThreshold(
        failedRatePercent,
        fallbackThresholds.walletFailureWarnPercent,
        fallbackThresholds.walletFailureCriticalPercent,
        totalValue <= 0
    );
    setSnapshotState("#snapshot-wallet-state", walletState);
    pushTrendPoint("wallet", totalValue);
    renderTrend("wallet", "#snapshot-wallet-trend");

    setSnapshotValue(
        "#snapshot-wallet-tpm",
        formatNumber(totalValue, 0) + " tx/min",
        "Success " + formatNumber(successValue, 0) + " / Failed " + formatNumber(failedValue, 0)
    );
}

function renderApiLatencySnapshot(httpMetric, metricName) {
    const totalSeconds = readMeasurement(httpMetric, ["TOTAL_TIME"]);
    const requestCount = readMeasurement(httpMetric, ["COUNT"]);
    const maxSeconds = readMeasurement(httpMetric, ["MAX"]);

    if (totalSeconds === null || requestCount === null || requestCount <= 0) {
        const label = isNotEmpty(metricName) ? metricName + " unavailable" : "No HTTP latency metric found";
        setSnapshotState("#snapshot-api-state", "UNKNOWN");
        renderTrend("apiLatency", "#snapshot-api-trend");
        setSnapshotValue("#snapshot-api-latency", "N/A", label);
        return;
    }

    const averageMs = (totalSeconds / requestCount) * 1000;
    const maxMs = maxSeconds === null ? null : (maxSeconds * 1000);
    const apiState = resolveStateByThreshold(
        averageMs,
        fallbackThresholds.apiLatencyWarnMs,
        fallbackThresholds.apiLatencyCriticalMs,
        false
    );
    setSnapshotState("#snapshot-api-state", apiState);
    pushTrendPoint("apiLatency", averageMs);
    renderTrend("apiLatency", "#snapshot-api-trend");
    const subText = "count " + formatNumber(requestCount, 0)
        + (maxMs === null ? "" : ", max " + formatNumber(maxMs, 2) + " ms");

    setSnapshotValue(
        "#snapshot-api-latency",
        formatNumber(averageMs, 2) + " ms",
        subText
    );
}

function renderDbPoolSnapshot(activeMetric, maxMetric, activeName, maxName) {
    const activeConnections = readMeasurement(activeMetric, ["VALUE"]);
    const maxConnections = readMeasurement(maxMetric, ["VALUE"]);

    if (activeConnections === null || maxConnections === null || maxConnections <= 0) {
        const source = isNotEmpty(activeName) || isNotEmpty(maxName)
            ? [activeName || "unknown", maxName || "unknown"].join(" / ")
            : "No DB pool metric found";
        setSnapshotState("#snapshot-db-state", "UNKNOWN");
        renderTrend("dbUtilization", "#snapshot-db-trend");
        setSnapshotValue("#snapshot-db-pool", "N/A", source);
        return;
    }

    const usagePercent = (activeConnections / maxConnections) * 100;
    const dbState = resolveStateByThreshold(
        usagePercent,
        fallbackThresholds.dbUtilizationWarnPercent,
        fallbackThresholds.dbUtilizationCriticalPercent,
        false
    );
    setSnapshotState("#snapshot-db-state", dbState);
    pushTrendPoint("dbUtilization", usagePercent);
    renderTrend("dbUtilization", "#snapshot-db-trend");
    setSnapshotValue(
        "#snapshot-db-pool",
        formatNumber(activeConnections, 0) + "/" + formatNumber(maxConnections, 0),
        formatNumber(usagePercent, 1) + "% active"
    );
}

function renderRedisSnapshot(redisCommandsMetric, cacheGetsMetric, metricName) {
    const redisCount = readMeasurement(redisCommandsMetric, ["COUNT"]);
    const redisTotalSeconds = readMeasurement(redisCommandsMetric, ["TOTAL_TIME"]);

    if (redisCount !== null && redisTotalSeconds !== null && redisCount > 0) {
        const averageMs = (redisTotalSeconds / redisCount) * 1000;
        const redisState = resolveStateByThreshold(
            averageMs,
            fallbackThresholds.redisLatencyWarnMs,
            fallbackThresholds.redisLatencyCriticalMs,
            false
        );
        setSnapshotState("#snapshot-redis-state", redisState);
        pushTrendPoint("redisLatency", averageMs);
        renderTrend("redisLatency", "#snapshot-redis-trend");
        setSnapshotValue(
            "#snapshot-redis-latency",
            formatNumber(averageMs, 2) + " ms",
            (metricName || "redis.command") + " count " + formatNumber(redisCount, 0)
        );
        return;
    }

    const cacheHitMissTotal = readMeasurement(cacheGetsMetric, ["COUNT", "VALUE"]);
    if (cacheHitMissTotal !== null) {
        setSnapshotState("#snapshot-redis-state", "IDLE");
        pushTrendPoint("redisLatency", 0);
        renderTrend("redisLatency", "#snapshot-redis-trend");
        setSnapshotValue(
            "#snapshot-redis-latency",
            formatNumber(cacheHitMissTotal, 0),
            "cache.gets total (fallback)"
        );
        return;
    }

    setSnapshotState("#snapshot-redis-state", "UNKNOWN");
    renderTrend("redisLatency", "#snapshot-redis-trend");
    setSnapshotValue("#snapshot-redis-latency", "N/A", (metricName || "redis.command") + " unavailable");
}

function setSnapshotValue(valueSelector, value, subText) {
    $(valueSelector).text(value);
    $(valueSelector + "-sub").text(subText);
}

function setSnapshotState(selector, state) {
    const normalized = String(firstNonBlank(state, "UNKNOWN")).toUpperCase();
    const css = resolveSnapshotStateClass(normalized);
    const $badge = $(selector);
    if ($badge.length === 0) {
        return;
    }
    $badge
        .removeClass("snapshot-state-healthy snapshot-state-warn snapshot-state-critical snapshot-state-idle snapshot-state-unknown")
        .addClass(css)
        .text(normalized);
}

function resolveSnapshotStateClass(state) {
    if (state === "HEALTHY") {
        return "snapshot-state-healthy";
    }
    if (state === "WARN") {
        return "snapshot-state-warn";
    }
    if (state === "CRITICAL") {
        return "snapshot-state-critical";
    }
    if (state === "IDLE") {
        return "snapshot-state-idle";
    }
    return "snapshot-state-unknown";
}

function setSnapshotThreshold(selector, label) {
    const $target = $(selector);
    if ($target.length === 0) {
        return;
    }
    $target.text(firstNonBlank(label, "Threshold: -"));
}

function setOverallSnapshotState(state) {
    const normalized = String(firstNonBlank(state, "UNKNOWN")).toUpperCase();
    const $badge = $("#snapshot-overall-state");
    if ($badge.length === 0) {
        return;
    }

    $badge.removeClass("badge-success badge-warning badge-danger badge-secondary");
    if (normalized === "HEALTHY") {
        $badge.addClass("badge-success");
    }
    else if (normalized === "WARN") {
        $badge.addClass("badge-warning");
    }
    else if (normalized === "CRITICAL") {
        $badge.addClass("badge-danger");
    }
    else {
        $badge.addClass("badge-secondary");
    }
    $badge.text("OVERALL: " + normalized);
}

function applySnapshotThresholds(thresholds) {
    const payload = thresholds || {};
    const walletThresholds = payload.wallet || {};
    const apiThresholds = payload.api || {};
    const dbThresholds = payload.database || {};
    const redisThresholds = payload.redis || {};

    setSnapshotThreshold(
        "#snapshot-wallet-threshold",
        "Threshold: WARN >= " + formatNumber(walletThresholds.failureWarnPercent, 1)
        + "%, CRITICAL >= " + formatNumber(walletThresholds.failureCriticalPercent, 1) + "%"
    );
    setSnapshotThreshold(
        "#snapshot-api-threshold",
        "Threshold: WARN >= " + formatNumber(apiThresholds.latencyWarnMs, 0)
        + " ms, CRITICAL >= " + formatNumber(apiThresholds.latencyCriticalMs, 0) + " ms"
    );
    setSnapshotThreshold(
        "#snapshot-db-threshold",
        "Threshold: WARN >= " + formatNumber(dbThresholds.utilizationWarnPercent, 0)
        + "%, CRITICAL >= " + formatNumber(dbThresholds.utilizationCriticalPercent, 0) + "%"
    );
    setSnapshotThreshold(
        "#snapshot-redis-threshold",
        "Threshold: WARN >= " + formatNumber(redisThresholds.latencyWarnMs, 0)
        + " ms, CRITICAL >= " + formatNumber(redisThresholds.latencyCriticalMs, 0) + " ms"
    );
}

function fallbackThresholdPayload() {
    return {
        wallet: {
            failureWarnPercent: fallbackThresholds.walletFailureWarnPercent,
            failureCriticalPercent: fallbackThresholds.walletFailureCriticalPercent
        },
        api: {
            latencyWarnMs: fallbackThresholds.apiLatencyWarnMs,
            latencyCriticalMs: fallbackThresholds.apiLatencyCriticalMs
        },
        database: {
            utilizationWarnPercent: fallbackThresholds.dbUtilizationWarnPercent,
            utilizationCriticalPercent: fallbackThresholds.dbUtilizationCriticalPercent
        },
        redis: {
            latencyWarnMs: fallbackThresholds.redisLatencyWarnMs,
            latencyCriticalMs: fallbackThresholds.redisLatencyCriticalMs
        }
    };
}

function resolveStateByThreshold(value, warnThreshold, criticalThreshold, idle) {
    if (idle) {
        return "IDLE";
    }
    if (!Number.isFinite(Number(value))) {
        return "UNKNOWN";
    }

    const numericValue = Number(value);
    if (numericValue >= Number(criticalThreshold || 0)) {
        return "CRITICAL";
    }
    if (numericValue >= Number(warnThreshold || 0)) {
        return "WARN";
    }
    return "HEALTHY";
}

function refreshOverallSnapshotStateFromTiles() {
    const states = [
        $("#snapshot-wallet-state").text(),
        $("#snapshot-api-state").text(),
        $("#snapshot-db-state").text(),
        $("#snapshot-redis-state").text()
    ];
    let overall = "HEALTHY";
    for (let i = 0; i < states.length; i++) {
        const state = String(states[i] || "").toUpperCase();
        if (rankState(state) > rankState(overall)) {
            overall = state;
        }
    }
    setOverallSnapshotState(overall);
}

function rankState(state) {
    const normalized = String(state || "").toUpperCase();
    if (normalized === "CRITICAL") {
        return 5;
    }
    if (normalized === "WARN") {
        return 4;
    }
    if (normalized === "UNKNOWN") {
        return 3;
    }
    if (normalized === "IDLE") {
        return 2;
    }
    if (normalized === "HEALTHY") {
        return 1;
    }
    return 0;
}

function pushTrendPoint(seriesKey, value) {
    if (!trendSeries.hasOwnProperty(seriesKey)) {
        return;
    }
    if (!Number.isFinite(Number(value))) {
        return;
    }
    const values = trendSeries[seriesKey];
    values.push(Number(value));
    if (values.length > MAX_TREND_POINTS) {
        values.shift();
    }
}

function renderTrend(seriesKey, containerSelector) {
    if (!trendSeries.hasOwnProperty(seriesKey)) {
        return;
    }
    const values = trendSeries[seriesKey];
    const $container = $(containerSelector);
    if ($container.length === 0) {
        return;
    }

    if (!Array.isArray(values) || values.length < 2) {
        $container.html("<div class='sparkline-empty'>Trend pending...</div>");
        return;
    }

    const width = Math.max(120, $container.width() || 120);
    const height = Math.max(30, $container.height() || 30);
    const min = Math.min.apply(null, values);
    const max = Math.max.apply(null, values);
    const spread = max - min <= 0 ? 1 : (max - min);
    const xStep = width / Math.max(1, values.length - 1);

    const linePoints = [];
    const areaPoints = [];
    areaPoints.push("0," + height);
    for (let i = 0; i < values.length; i++) {
        const x = Math.round(i * xStep * 100) / 100;
        const normalized = (values[i] - min) / spread;
        const y = Math.round((height - (normalized * (height - 4) + 2)) * 100) / 100;
        linePoints.push(x + "," + y);
        areaPoints.push(x + "," + y);
    }
    areaPoints.push(width + "," + height);

    $container.html(
        "<svg class='sparkline-svg' viewBox='0 0 " + width + " " + height + "' preserveAspectRatio='none'>"
        + "<polygon class='sparkline-area' points='" + areaPoints.join(" ") + "'></polygon>"
        + "<polyline class='sparkline-line' points='" + linePoints.join(" ") + "'></polyline>"
        + "</svg>"
    );
}

function isRedisInspectorAvailable() {
    return $(REDIS_INSPECTOR_CARD_SELECTOR).length > 0;
}

function getRedisInspectorConfig() {
    const $card = $(REDIS_INSPECTOR_CARD_SELECTOR);
    return {
        summaryUrl: firstNonBlank(
            $card.attr("data-summary-url"),
            buildApplicationEndpointUrl("/api/web/sec/settings/actuator/redis/summary")
        ),
        keysUrl: firstNonBlank(
            $card.attr("data-keys-url"),
            buildApplicationEndpointUrl("/api/web/sec/settings/actuator/redis/keys")
        ),
        deleteUrl: firstNonBlank(
            $card.attr("data-delete-url"),
            buildApplicationEndpointUrl("/api/web/sec/settings/actuator/redis/delete")
        ),
        canSummary: toBoolean($card.attr("data-can-summary")),
        canSearch: toBoolean($card.attr("data-can-search")),
        canDelete: toBoolean($card.attr("data-can-delete"))
    };
}

function applyRedisPermissionVisibility() {
    const config = getRedisInspectorConfig();
    const canDelete = !!config.canDelete;

    if (canDelete) {
        $(".redis-select-col, .redis-delete-col").removeClass("d-none");
        $("#btn-redis-delete-selected").removeClass("d-none");
    }
    else {
        $(".redis-select-col, .redis-delete-col").addClass("d-none");
        $("#btn-redis-delete-selected").addClass("d-none");
        $("#redis-select-all").prop("checked", false);
    }
}

function refreshRedisSummary(isManualRefresh) {
    if (!isRedisInspectorAvailable()) {
        return;
    }

    const config = getRedisInspectorConfig();
    const $refreshButton = $(REDIS_SUMMARY_REFRESH_BUTTON_SELECTOR);
    if (!config.canSummary || $refreshButton.prop("disabled")) {
        return;
    }

    $refreshButton.prop("disabled", true);

    $.ajax({
        url: config.summaryUrl,
        method: "GET",
        dataType: "json",
        cache: false,
        global: false
    })
        .done(function (response) {
            renderRedisSummary(response);
            $("#redis-summary-fetched-at").text("Last summary: " + new Date().toLocaleString());
            if (isManualRefresh) {
                const ok = String(response && response.status ? response.status : "").toUpperCase() === "OK";
                if (ok) {
                    notify("Success", "Redis summary refreshed.", "success");
                }
                else {
                    notify("Error", firstNonBlank(response && response.message, "Redis summary unavailable."), "error");
                }
            }
        })
        .fail(function (xhr) {
            if (handleUnauthorizedApiResponse(xhr, true)) {
                return;
            }

            const message = isNotEmpty(xhr && xhr.responseText)
                ? xhr.responseText
                : "Failed to load Redis summary.";
            setRedisSummaryStatusBadge("ERROR", "Summary request failed");
            if (isManualRefresh) {
                notify("Error", message, "error");
            }
        })
        .always(function () {
            $refreshButton.prop("disabled", !config.canSummary);
        });
}

function searchRedisKeys(isManualSearch) {
    if (!isRedisInspectorAvailable()) {
        return;
    }

    const config = getRedisInspectorConfig();
    if (!config.canSearch) {
        renderRedisTableMessage("Key search permission is not assigned.", false);
        return;
    }

    const pattern = String($("#redis-key-pattern").val() || "").trim();
    const limit = Number($("#redis-key-limit").val() || 100);
    setRedisTableLoading(true);

    $.ajax({
        url: config.keysUrl,
        method: "GET",
        dataType: "json",
        cache: false,
        global: false,
        data: {
            pattern: pattern,
            limit: limit
        }
    })
        .done(function (response) {
            renderRedisTableResults(response);
            if (isManualSearch) {
                const ok = String(response && response.status ? response.status : "").toUpperCase() === "OK";
                if (ok) {
                    notify("Success", "Redis keys loaded.", "success");
                }
                else {
                    notify("Error", firstNonBlank(response && response.message, "Redis key search failed."), "error");
                }
            }
        })
        .fail(function (xhr) {
            if (handleUnauthorizedApiResponse(xhr, true)) {
                return;
            }

            const message = isNotEmpty(xhr && xhr.responseText)
                ? xhr.responseText
                : "Failed to load Redis keys.";
            renderRedisTableMessage("Failed to load Redis keys.", true);
            if (isManualSearch) {
                notify("Error", message, "error");
            }
        })
        .always(function () {
            setRedisTableLoading(false);
            setRedisDeleteButtonState();
        });
}

function deleteRedisKeys(keys, confirmation) {
    if (!isRedisInspectorAvailable()) {
        return;
    }

    const config = getRedisInspectorConfig();
    if (!config.canDelete) {
        return;
    }

    const normalizedKeys = Array.isArray(keys)
        ? keys.map(function (item) {
            return String(item || "").trim();
        }).filter(function (item) {
            return isNotEmpty(item);
        })
        : [];

    if (normalizedKeys.length === 0) {
        return;
    }

    const $deleteButton = $("#btn-redis-delete-selected");
    $deleteButton.prop("disabled", true);

    $.ajax({
        url: config.deleteUrl,
        method: "POST",
        contentType: "application/json",
        dataType: "json",
        cache: false,
        global: false,
        headers: buildCsrfHeaders(),
        data: JSON.stringify({
            keys: normalizedKeys,
            confirmation: confirmation || ""
        })
    })
        .done(function (response) {
            const status = String(response && response.status ? response.status : "").toUpperCase();
            const message = firstNonBlank(response && response.message, "Redis deletion completed.");

            if (status === "ERROR") {
                notify("Error", message, "error");
                return;
            }
            if (status === "WARN" && response && response.confirmationRequired) {
                const expected = firstNonBlank(response.confirmationHint, "");
                notify("Warning", message + (isNotEmpty(expected) ? " (Required: " + expected + ")" : ""), "warning");
                return;
            }

            const deleted = Number(response && response.deleted ? response.deleted : 0);
            notify("Success", "Deleted " + formatNumber(deleted, 0) + " Redis key(s).", "success");
            refreshRedisSummary(false);
            searchRedisKeys(false);
        })
        .fail(function (xhr) {
            if (handleUnauthorizedApiResponse(xhr, true)) {
                return;
            }

            const message = isNotEmpty(xhr && xhr.responseText)
                ? xhr.responseText
                : "Failed to delete selected Redis keys.";
            notify("Error", message, "error");
        })
        .always(function () {
            $deleteButton.prop("disabled", false);
            setRedisDeleteButtonState();
        });
}

function requestBulkDeleteConfirmation(selectedCount) {
    if (!Number.isFinite(selectedCount) || selectedCount < BULK_DELETE_CONFIRMATION_THRESHOLD) {
        return "";
    }

    const expected = "DELETE " + selectedCount;
    const typed = window.prompt(
        "Bulk delete protection is enabled.\nType `" + expected + "` to confirm this deletion.",
        ""
    );

    if (typed === null) {
        return null;
    }
    if (String(typed).trim() !== expected) {
        notify("Warning", "Bulk delete canceled. Confirmation text did not match.", "warning");
        return null;
    }
    return expected;
}

function renderRedisSummary(response) {
    const payload = response || {};
    const status = String(payload.status || "ERROR").toUpperCase();

    if (status !== "OK") {
        setRedisSummaryStatusBadge("ERROR", firstNonBlank(payload.message, "Redis summary unavailable"));
        setRedisSummaryValue("#redis-summary-used-memory", "N/A", "Unavailable");
        setRedisSummaryValue("#redis-summary-max-memory", "N/A", "Unavailable");
        setRedisSummaryValue("#redis-summary-remaining-memory", "N/A", "Unavailable");
        setRedisSummaryValue("#redis-summary-total-keys", "N/A", "Unavailable");
        return;
    }

    const memory = payload.memory || {};
    const keys = payload.keys || {};
    const server = payload.server || {};
    const limitSource = String(memory.limitSource || "").toUpperCase();

    const usedHuman = firstNonBlank(memory.usedHuman, "N/A");
    let usedPercentText = isFiniteNumber(memory.usedPercent)
        ? formatNumber(memory.usedPercent, 2) + "% of max"
        : "No max memory limit";

    if (limitSource === "SYSTEM_MEMORY" && isFiniteNumber(memory.usedPercent)) {
        usedPercentText = formatNumber(memory.usedPercent, 2) + "% of system memory";
    }
    else if (limitSource === "UNLIMITED") {
        usedPercentText = "Redis maxmemory is unlimited";
    }

    const maxHuman = firstNonBlank(memory.maxHuman, "N/A");
    const remainingHuman = firstNonBlank(memory.remainingHuman, "N/A");

    const totalKeys = Number.isFinite(Number(keys.totalKeys))
        ? formatNumber(keys.totalKeys, 0)
        : "N/A";

    const expiringKeys = Number.isFinite(Number(keys.expiringKeys))
        ? formatNumber(keys.expiringKeys, 0)
        : "N/A";

    const avgTtlMs = Number.isFinite(Number(keys.avgTtlMs))
        ? formatRedisMs(Number(keys.avgTtlMs))
        : "N/A";
    const hitRatePercent = Number(keys.hitRatePercent);
    const instantaneousOpsPerSec = Number(server.instantaneousOpsPerSec);
    const connectedClients = Number(server.connectedClients);
    const blockedClients = Number(server.blockedClients);
    const memFragmentationRatio = Number(server.memFragmentationRatio);

    if (isFiniteNumber(memory.usedPercent)) {
        pushTrendPoint("redisUsedMemoryPercent", Number(memory.usedPercent));
    }
    renderTrend("redisUsedMemoryPercent", "#redis-summary-used-memory-trend");

    if (Number.isFinite(Number(keys.totalKeys))) {
        pushTrendPoint("redisTotalKeys", Number(keys.totalKeys));
    }
    renderTrend("redisTotalKeys", "#redis-summary-total-keys-trend");

    setRedisSummaryValue("#redis-summary-used-memory", usedHuman, usedPercentText);

    let maxMemorySubText = "Configured memory ceiling";
    if (limitSource === "SYSTEM_MEMORY") {
        maxMemorySubText = "Using host memory as reference";
    }
    else if (limitSource === "UNLIMITED") {
        maxMemorySubText = "No maxmemory limit configured";
    }
    if (isFiniteNumber(memFragmentationRatio)) {
        maxMemorySubText += ", frag " + formatNumber(memFragmentationRatio, 2) + "x";
    }
    setRedisSummaryValue("#redis-summary-max-memory", maxHuman, maxMemorySubText);

    let remainingSubText = "Available memory estimate";
    if (limitSource === "UNLIMITED") {
        remainingSubText = "Unlimited (no configured ceiling)";
    }
    else if (limitSource === "SYSTEM_MEMORY") {
        remainingSubText = "Estimated from host total memory";
    }
    if (isFiniteNumber(instantaneousOpsPerSec) || isFiniteNumber(connectedClients)) {
        const opsText = isFiniteNumber(instantaneousOpsPerSec)
            ? "ops/s " + formatNumber(instantaneousOpsPerSec, 0)
            : "";
        const clientsText = isFiniteNumber(connectedClients)
            ? "clients " + formatNumber(connectedClients, 0)
            : "";
        const blockedText = isFiniteNumber(blockedClients) && blockedClients > 0
            ? "blocked " + formatNumber(blockedClients, 0)
            : "";
        const extra = [opsText, clientsText, blockedText].filter(function (text) {
            return isNotEmpty(text);
        }).join(", ");
        if (isNotEmpty(extra)) {
            remainingSubText += " | " + extra;
        }
    }
    setRedisSummaryValue("#redis-summary-remaining-memory", remainingHuman, remainingSubText);

    let keysSubText = "Expiring " + expiringKeys + ", Avg TTL " + avgTtlMs;
    if (isFiniteNumber(hitRatePercent)) {
        keysSubText += ", Hit Rate " + formatNumber(hitRatePercent, 2) + "%";
    }
    setRedisSummaryValue("#redis-summary-total-keys", totalKeys, keysSubText);
    setRedisSummaryStatusBadge("OK", "Redis summary loaded");
}

function renderRedisTableResults(response) {
    const payload = response || {};
    const status = String(payload.status || "ERROR").toUpperCase();

    if (status !== "OK") {
        renderRedisTableMessage(firstNonBlank(payload.message, "Redis key search failed."), true);
        return;
    }

    const rows = Array.isArray(payload.keys) ? payload.keys : [];
    const canDelete = !!getRedisInspectorConfig().canDelete;
    const htmlRows = [];

    if (rows.length === 0) {
        renderRedisTableMessage("No Redis keys matched the pattern.", false);
        $("#redis-key-table-meta").text(
            "Pattern: " + firstNonBlank(payload.pattern, "*") + " | Keys: 0"
        );
        return;
    }

    for (let i = 0; i < rows.length; i++) {
        const row = rows[i] || {};
        const key = String(row.key || "");
        const keyDisplay = formatRedisKeyForDisplay(key);
        const dataType = firstNonBlank(row.dataType, "unknown");
        const ttlLabel = firstNonBlank(row.ttlLabel, "N/A");
        const expireAt = firstNonBlank(row.expireAt, "-");

        if (canDelete) {
            htmlRows.push(
                "<tr>"
                + "<td class='redis-select-col text-center'>"
                + "<div class='pretty p-default p-pulse m-0'>"
                + "<input class='js-redis-row-check' data-key='"
                + escapeHtml(key)
                + "' type='checkbox'>"
                + "<div class='state p-default-o'><label></label></div>"
                + "</div>"
                + "</td>"
                + "<td class='wrap-white-space redis-key-cell'><code title='"
                + escapeHtml(key)
                + "'>"
                + escapeHtml(keyDisplay)
                + "</code></td>"
                + "<td><span class='badge badge-light border text-uppercase'>"
                + escapeHtml(dataType)
                + "</span></td>"
                + "<td>" + escapeHtml(ttlLabel) + "</td>"
                + "<td>" + escapeHtml(expireAt) + "</td>"
                + "<td class='redis-delete-col'>"
                + "<button class='btn btn-xs btn-outline-danger js-redis-delete-one' data-key='"
                + escapeHtml(key)
                + "' type='button'>Delete</button>"
                + "</td>"
                + "</tr>"
            );
        }
        else {
            htmlRows.push(
                "<tr>"
                + "<td class='wrap-white-space redis-key-cell'><code title='"
                + escapeHtml(key)
                + "'>"
                + escapeHtml(keyDisplay)
                + "</code></td>"
                + "<td><span class='badge badge-light border text-uppercase'>"
                + escapeHtml(dataType)
                + "</span></td>"
                + "<td>" + escapeHtml(ttlLabel) + "</td>"
                + "<td>" + escapeHtml(expireAt) + "</td>"
                + "</tr>"
            );
        }
    }

    $("#redis-key-table-body").html(htmlRows.join(""));
    applyRedisPermissionVisibility();
    setRedisDeleteButtonState();

    const hasMore = !!payload.hasMore;
    const message = "Pattern: " + firstNonBlank(payload.pattern, "*")
        + " | Returned: " + formatNumber(payload.returned, 0)
        + (hasMore ? " (more keys available)" : "");
    $("#redis-key-table-meta").text(message);
}

function renderRedisTableMessage(message, isError) {
    const canDelete = !!getRedisInspectorConfig().canDelete;
    const columnCount = canDelete ? 6 : 4;
    const cssClass = isError ? "text-danger" : "text-muted";

    $("#redis-key-table-body").html(
        "<tr><td class='" + cssClass + "' colspan='" + columnCount + "'>"
        + escapeHtml(firstNonBlank(message, "No data"))
        + "</td></tr>"
    );

    applyRedisPermissionVisibility();
    setRedisDeleteButtonState();
}

function setRedisDeleteButtonState() {
    const config = getRedisInspectorConfig();
    const $button = $("#btn-redis-delete-selected");
    if ($button.length === 0) {
        return;
    }

    if (!config.canDelete) {
        $button.prop("disabled", true);
        return;
    }

    const selectedCount = $("#redis-key-table-body .js-redis-row-check:checked").length;
    const label = selectedCount > 0
        ? "Delete Selected (" + selectedCount + ")"
        : "Delete Selected";

    $button.html("<i class='far fa-trash-alt mr-1'></i> " + label);
    $button.prop("disabled", selectedCount === 0);
}

function setRedisTableLoading(isLoading) {
    if (!isLoading) {
        return;
    }
    renderRedisTableMessage("Loading Redis keys...", false);
}

function setRedisSummaryValue(valueSelector, value, subText) {
    $(valueSelector).text(value);
    $(valueSelector + "-sub").text(subText);
}

function setRedisSummaryStatusBadge(status, message) {
    const normalizedStatus = String(status || "").toUpperCase();
    const $badge = $("#redis-summary-status");
    $badge.removeClass("badge-secondary badge-success badge-warning badge-danger");

    if (normalizedStatus === "OK") {
        $badge.addClass("badge-success").text("OK");
    }
    else if (normalizedStatus === "WARN") {
        $badge.addClass("badge-warning").text("WARN");
    }
    else {
        $badge.addClass("badge-danger").text("ERROR");
    }

    if (isNotEmpty(message)) {
        $("#redis-summary-fetched-at").text(message + " | " + new Date().toLocaleString());
    }
}

function formatRedisMs(value) {
    if (!isFiniteNumber(value)) {
        return "N/A";
    }

    const ms = Number(value);
    if (ms < 1000) {
        return formatNumber(ms, 0) + " ms";
    }

    const seconds = ms / 1000;
    if (seconds < 60) {
        return formatNumber(seconds, 2) + " s";
    }

    const minutes = seconds / 60;
    return formatNumber(minutes, 2) + " min";
}

function readMeasurement(metricResponse, statistics) {
    if (!metricResponse || !Array.isArray(metricResponse.measurements) || metricResponse.measurements.length === 0) {
        return null;
    }

    const wanted = Array.isArray(statistics) ? statistics : [];
    for (let i = 0; i < wanted.length; i++) {
        const candidate = String(wanted[i] || "").toUpperCase();
        const matched = metricResponse.measurements.find(function (m) {
            return String(m && m.statistic ? m.statistic : "").toUpperCase() === candidate;
        });
        if (matched && Number.isFinite(Number(matched.value))) {
            return Number(matched.value);
        }
    }

    const first = metricResponse.measurements[0];
    if (first && Number.isFinite(Number(first.value))) {
        return Number(first.value);
    }

    return null;
}

function formatNumber(value, digits) {
    if (!Number.isFinite(Number(value))) {
        return "-";
    }
    return Number(value).toLocaleString(undefined, {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits
    });
}

function isFiniteNumber(value) {
    return Number.isFinite(Number(value));
}

function resolveFirstMetricName(metricNames, candidates) {
    if (!Array.isArray(metricNames) || metricNames.length === 0 || !Array.isArray(candidates)) {
        return "";
    }

    for (let i = 0; i < candidates.length; i++) {
        if (metricNames.indexOf(candidates[i]) >= 0) {
            return candidates[i];
        }
    }
    return "";
}

function buildActuatorEndpointUrl(pathSuffix) {
    const basePath = resolveActuatorBasePath().replace(/\/+$/, "");
    const suffix = String(pathSuffix || "").replace(/^\/+/, "");
    return basePath + "/" + suffix;
}

function buildApplicationEndpointUrl(pathSuffix) {
    const basePath = resolveApplicationBasePath().replace(/\/+$/, "");
    const suffix = String(pathSuffix || "").replace(/^\/+/, "");

    if (!isNotEmpty(basePath)) {
        return "/" + suffix;
    }
    return basePath + "/" + suffix;
}

function buildCsrfHeaders() {
    const csrfToken = String($("meta[name='_csrf']").attr("content") || "").trim();
    const csrfHeader = String($("meta[name='_csrf_header']").attr("content") || "").trim();
    const headers = {};

    if (isNotEmpty(csrfToken) && isNotEmpty(csrfHeader)) {
        headers[csrfHeader] = csrfToken;
    }

    headers["X-Requested-With"] = "XMLHttpRequest";
    return headers;
}

function resolveApplicationBasePath() {
    const endpointUrl = firstNonBlank(
        selectedEndpointUrl,
        $("#actuator-endpoint-list .js-actuator-endpoint").first().attr("data-endpoint"),
        window.location.pathname
    );

    const actuatorMarker = "/actuator";
    const actuatorIndex = endpointUrl.indexOf(actuatorMarker);
    if (actuatorIndex >= 0) {
        return endpointUrl.substring(0, actuatorIndex);
    }

    const webMarker = "/web/";
    const webIndex = endpointUrl.indexOf(webMarker);
    if (webIndex >= 0) {
        return endpointUrl.substring(0, webIndex);
    }

    return "";
}

function resolveActuatorBasePath() {
    const endpointUrl = firstNonBlank(
        selectedEndpointUrl,
        $("#actuator-endpoint-list .js-actuator-endpoint").first().attr("data-endpoint"),
        DEFAULT_ACTUATOR_BASE
    );

    const marker = "/actuator";
    const markerIndex = endpointUrl.indexOf(marker);
    if (markerIndex < 0) {
        return DEFAULT_ACTUATOR_BASE;
    }
    return endpointUrl.substring(0, markerIndex + marker.length);
}

function firstNonBlank() {
    for (let i = 0; i < arguments.length; i++) {
        const candidate = arguments[i];
        if (isNotEmpty(candidate)) {
            return candidate;
        }
    }
    return "";
}

function toBoolean(value) {
    if (typeof value === "boolean") {
        return value;
    }
    const normalized = String(value || "").trim().toLowerCase();
    return normalized === "true" || normalized === "1" || normalized === "yes";
}

function getNowMs() {
    if (window.performance && typeof window.performance.now === "function") {
        return window.performance.now();
    }
    return Date.now();
}

function applySyntaxHighlight(codeElement, content) {
    if (!codeElement || !window.hljs || typeof window.hljs.highlightElement !== "function") {
        return;
    }

    // Skip expensive highlighting on very large payloads (e.g. heavy metrics pages).
    if ((content || "").length > 300000) {
        return;
    }

    codeElement.removeAttribute("data-highlighted");
    window.hljs.highlightElement(codeElement);
}

function countLines(content) {
    if (!isNotEmpty(content)) {
        return 0;
    }
    return String(content).split(/\r\n|\r|\n/).length;
}

function formatPayloadSize(content) {
    const bytes = getUtf8ByteLength(content);
    if (bytes < 1024) {
        return bytes + " B";
    }

    const kb = bytes / 1024;
    if (kb < 1024) {
        return kb.toFixed(2) + " KB";
    }

    const mb = kb / 1024;
    if (mb < 1024) {
        return mb.toFixed(2) + " MB";
    }

    const gb = mb / 1024;
    return gb.toFixed(2) + " GB";
}

function getUtf8ByteLength(content) {
    const source = content || "";
    if (window.TextEncoder) {
        return new TextEncoder().encode(source).length;
    }
    return source.length;
}
