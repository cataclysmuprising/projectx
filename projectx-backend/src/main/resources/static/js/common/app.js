/**
 * @file app.js
 * @description This js file initiate all global settings for website. This file
 *              may include in most of pages.
 * @version 3.0
 * @author Than Htike Aung
 * @contact thanhtikeaung@tamantaw.com
 * @copyright Copyright (c) 2017-2018, Than Htike Aung. This source file is free
 *            software, available under the following license: MIT license. See
 *            the license file for details.
 */

/**
 * ################################## # Global Variables ##################################
 */
let ROW_PER_PAGE = 20;
let SECONDARY_ROW_PER_PAGE = 10;
let FILE_SIZE_UNITS = ['Bytes', 'KB', 'MB', 'GB'];
let PAGE_MODE = "";
/**
 * ################################## # JS working functions on page ##################################
 */
$.ajaxSetup({
    dataType: "json",
    contentType: "application/json; charset=utf-8",
    timeout: 100000,
});
$.ajaxPrefilter((options, originalOptions, jqXHR) => {
    const existingBeforeSend = options.beforeSend;
    options.beforeSend = function (xhr, settings) {
        applyDefaultAjaxHeaders(xhr, settings);
        if (typeof existingBeforeSend === "function") {
            return existingBeforeSend.call(this, xhr, settings);
        }
        return undefined;
    };
});
$(document).ajaxComplete((event, xhr, settings) => {
    // if (xhr.status === 208 || xhr.status === 226) {
    //     document.write(xhr.responseText);
    // }
}).ajaxError((event, xhr, settings) => {
    if (handleUnauthorizedApiResponse(xhr, true)) {
        return;
    }
    notify("Error !", "Something went wrong with requesting data to server.", "error");
});

/**
 * ################################## # Application's Main Functions ##################################
 */
$(function () {
    PAGE_MODE = $('#pageMode').val();
    baseInit();
    baseBind();
    $('[data-toggle="tooltip"]').tooltip();
    if (typeof init === "function") {
        init();
    }
    if (typeof bind === "function") {
        bind();
    }
});

function baseInit() {
    $('.disabled').attr('tabindex', '-1');
    $('.disabled input').attr('tabindex', '-1');
    $('form,input').attr('autocomplete', 'off');
    $('form').trigger("reset");
    initJQueryDataTable();
    initSelectPickers();
    initToastrAlertOptions();
    initJQueryValidator();
    loadValidationErrors();
    initPageMessage();
}

function baseBind() {
    $('form').on('reset', function (e) {
        setTimeout(function () {
            let selectpicker = $(".selectpicker,.dropdown-select");
            if (selectpicker.length > 0) {
                selectpicker.selectpicker('refresh');
                selectpicker.trigger('refreshed.bs.select');
            }
        });
    });

    $('#btnSignInOut').on('click', function (e) {
        e.preventDefault();
        let title = $(this).attr('title') || '';
        if (title.toLowerCase().includes('sign out')) {
            const token = $('meta[name="_csrf"]').attr('content');
            const header = $('meta[name="_csrf_header"]').attr('content');

            $.ajax({
                url: getContextPath() + '/web/sec/logout',
                type: 'POST',
                beforeSend: function (xhr) {
                    xhr.setRequestHeader(header, token);
                },
                success: function () {
                    window.location.href = getContextPath() + '/web/pub/login';
                },
                error: function () {
                    // fallback
                    window.location.href = getContextPath() + '/web/pub/login';
                }
            });
        }
        else {
            window.location.href = getContextPath() + '/web/pub/login';
        }
    });

    // Wallet integrity validation (admin-only action in header).
    $('#btnWalletValidateHeader').on('click', function (e) {
        e.preventDefault();
        $.ajax({
            url: getApiResourcePath() + 'sec/wallet_admin/monitoring/validate',
            type: 'POST',
            data: JSON.stringify({}),
            success: function (response) {
                const chainViolation = Number(response?.chainViolation || 0);
                if (chainViolation > 0) {
                    notify(
                        "Warning !",
                        "Wallet constraints validated, but chain violations remain: " + chainViolation + ".",
                        "warning"
                    );
                    return;
                }
                notify("Success !", "Wallet constraints + chain validated.", "success");
            },
            error: function () {
                notify("Error !", "Wallet constraint validation failed.", "error");
            }
        });
    });

    disableFormSubmitEvent();
}

/**
 * ################################## # Application's Initializings Functions ##################################
 */

function initPageMessage() {
    if ($("#pageMessage").length > 0) {
        let pageMessage = $("#pageMessage");
        notify($(pageMessage).attr("data-title"), $(pageMessage).attr("data-info"), $(pageMessage).attr("data-style"));
    }
}

function initJQueryDataTable() {
    if ($.fn.DataTable) {
        Object.assign($.fn.dataTable.defaults, {

            serverSide: true,
            processing: true,
            lengthChange: false,
            searching: false,
            pagingType: "first_last_numbers",
            pageLength: ROW_PER_PAGE,
            aaSorting: [],
            /*scrollX: true,*/
            ajax: {

                // 1️⃣ Capture draw per table (SAFE)
                data: function (d) {
                    // `this` is the DataTables settings object
                    this._lastDraw = d.draw;
                    const reqLength = Number(d.length);
                    const reqStart = Number(d.start);
                    this._requestedPageIndex =
                        Number.isFinite(reqLength) && reqLength > 0
                        && Number.isFinite(reqStart) && reqStart >= 0
                            ? Math.floor(reqStart / reqLength)
                            : 0;
                    return JSON.stringify(d);
                },

                // 2️⃣ Fix response BEFORE DT parses it
                dataFilter: function (data) {

                    let json = JSON.parse(data);

                    const total = Number(json.totalElements ?? 0);
                    const rawPageNumber = Number(json.pageNumber);
                    const requestedPageIndex = Number(this._requestedPageIndex);

                    // Normalize mixed page-number contracts:
                    // - persistence now returns 0-based pageNumber
                    // - legacy consumers may still emit 1-based
                    let normalizedPageIndex =
                        Number.isInteger(requestedPageIndex) && requestedPageIndex >= 0
                            ? requestedPageIndex
                            : 0;

                    if (Number.isInteger(rawPageNumber)) {
                        if (rawPageNumber === normalizedPageIndex + 1) {
                            normalizedPageIndex = Math.max(0, rawPageNumber - 1);
                        }
                        else if (rawPageNumber >= 0) {
                            normalizedPageIndex = rawPageNumber;
                        }
                    }

                    // UI stays 1-based, technical index remains 0-based.
                    json.pageIndex = normalizedPageIndex;
                    json.pageNumber = normalizedPageIndex + 1;

                    // Required DT fields
                    json.recordsTotal = total;
                    json.recordsFiltered = total;

                    // 🔥 Echo back the CORRECT draw for THIS table
                    json.draw = this._lastDraw;

                    // Ensure data array
                    if (!Array.isArray(json.data)) {
                        json.data = [];
                    }

                    return JSON.stringify(json);
                },

                // 3️⃣ Extract rows only
                dataSrc: function (json) {
                    return json.data;
                }
            },

            infoCallback: function (settings, start, end, max, total) {

                if (total === 0) {
                    return "Empty records";
                }

                const pageSize = Number(settings?._iDisplayLength || ROW_PER_PAGE);
                const resolvedPageSize = Number.isFinite(pageSize) && pageSize > 0 ? pageSize : total;

                let safeStart = start;
                let safeEnd = end;

                // Guard against out-of-range client page state, e.g. URL asks page=999.
                if (safeStart > total) {
                    safeStart = Math.floor((total - 1) / resolvedPageSize) * resolvedPageSize + 1;
                    safeEnd = total;
                }
                else {
                    safeStart = Math.max(1, safeStart);
                    safeEnd = Math.min(total, Math.max(safeStart, safeEnd));
                }

                if (total !== max) {
                    return "Showing " + safeStart + " to " + safeEnd +
                        " of " + total + " records (filtered from " + max + ")";
                }

                return "Showing " + safeStart + " to " + safeEnd + " of " + total + " records";
            },

            language: {
                sProcessing: "&nbsp;",
                sZeroRecords: "No matching records found.",
                sEmptyTable: "No matching records found.",
                sLoadingRecords: "&nbsp;"
            }
        });

        // Read initial page from URL once before each server-side table first draw.
        $(document)
            .off('preInit.dt.pageQuery')
            .on('preInit.dt.pageQuery', function (e, settings) {
                if (!settings?.oFeatures?.bServerSide) {
                    return;
                }

                const pageNumber = getDataTablePageNumberFromUrl();
                if (pageNumber <= 1) {
                    return;
                }

                const pageSize = Number(settings._iDisplayLength || settings.oInit?.pageLength || ROW_PER_PAGE);
                if (!Number.isFinite(pageSize) || pageSize <= 0) {
                    return;
                }

                const displayStart = (pageNumber - 1) * pageSize;
                settings._iDisplayStart = displayStart;
                settings.iInitDisplayStart = displayStart;
            });

        // Keep URL query in sync with current DataTable page.
        $(document)
            .off('draw.dt.pageQuery')
            .on('draw.dt.pageQuery', function (e, settings) {
                if (!settings?.oFeatures?.bServerSide) {
                    return;
                }

                const api = new $.fn.dataTable.Api(settings);
                const pageInfo = api.page.info();
                if (!pageInfo) {
                    return;
                }

                // Auto-clamp invalid page (e.g. URL page beyond total pages).
                if (pageInfo.recordsDisplay > 0 && pageInfo.length > 0) {
                    const maxPageIndex = Math.max(0, Math.ceil(pageInfo.recordsDisplay / pageInfo.length) - 1);
                    if (pageInfo.page > maxPageIndex) {
                        api.page(maxPageIndex).draw('page');
                        return;
                    }
                }

                syncDataTablePageQueryParam(pageInfo.page + 1);
            });

        $(window).resize(function () {
            if (this.resizeTO) {
                clearTimeout(this.resizeTO);
            }
            this.resizeTO = setTimeout(function () {
                $(this).trigger('resizeEnd');
            }, 500);
        });

        $(window).bind('resizeEnd', function () {
            $(".datatable_scrollArea").scroll();
        });
    }

    // $('a[data-bs-toggle="tab"],a[data-toggle="pill"]').on('shown.bs.tab', function () {
    //     $.fn.dataTable.tables({visible: true, api: true})
    //         .columns.adjust()
    //         .draw(false);
    // });
}

function initSelectPickers(scope = null) {

    let $selects;

    // ----------------------------------------
    // 1️⃣ Determine targets
    // ----------------------------------------
    if (!scope) {
        // Global init → ONLY selectpicker
        $selects = $('select.selectpicker');
    }
    else {
        // Manual init → selector OR element
        $selects = $(scope).is('select')
            ? $(scope)
            : $(scope).filter('select').add($(scope).find('select'));
    }

    // ----------------------------------------
    // 2️⃣ Init each select safely
    // ----------------------------------------
    $selects.each(function () {

        const $select = $(this);

        // Prevent double init
        if ($select.data('__selectpicker_inited__')) return;

        // Normalize
        $select
            .addClass('selectpicker')
            .data('__selectpicker_inited__', true);

        // Init plugin
        $select.selectpicker();

        // ----------------------------------------
        // 3️⃣ One-time selection resolver
        // ----------------------------------------
        const resolveSelection = () => {

            if ($select.data('__selection_resolved__')) return;

            // Async guard
            if (
                $select.data('async') &&
                $select.find('option').length === 0
            ) {
                return;
            }

            // Native value wins
            const currentVal = $select.val();
            if (
                currentVal !== null &&
                currentVal !== '' &&
                !(Array.isArray(currentVal) && currentVal.length === 0)
            ) {
                finalize();
                return;
            }

            // Fallback → data-selected
            const dataSelected = $select.attr('data-selected');
            if (!dataSelected) {
                finalize();
                return;
            }

            const values = String(dataSelected)
                .split(',')
                .map(v => v.trim())
                .filter(Boolean);

            if (!values.length) {
                finalize();
                return;
            }

            finalize();

            // Apply safely (escape recursion)
            setTimeout(() => {
                $select.selectpicker('val', values);
                $select.selectpicker('refresh');
                $select.trigger('change');
            }, 0);
        };

        const finalize = () => {
            $select
                .data('__selection_resolved__', true)
                // .removeAttr('data-selected')
                .off(
                    'loaded.bs.select rendered.bs.select refreshed.bs.select',
                    resolveSelection
                );
        };

        // ----------------------------------------
        // 4️⃣ Listen once (init + async refresh)
        // ----------------------------------------
        $select.on(
            'loaded.bs.select rendered.bs.select refreshed.bs.select',
            resolveSelection
        );
    });
}

function initToastrAlertOptions() {
    if (typeof toastr !== 'undefined') {
        toastr.options = {
            "closeButton": false,
            "debug": false,
            "newestOnTop": true,
            "progressBar": true,
            "positionClass": "toast-bottom-right",
            "preventDuplicates": true,
            "onclick": null,
            "showDuration": "3000",
            "hideDuration": "1000",
            "timeOut": "8000",
            "extendedTimeOut": "3000",
            "showEasing": "swing",
            "hideEasing": "linear",
            "showMethod": "fadeIn",
            "hideMethod": "fadeOut"
        }
    }
}

function initJQueryValidator() {
    if ($.fn.validate) {
        $.validator.setDefaults({
            errorElement: "div",
            errorClass: "invalid-feedback",
            ignore: [],
            errorPlacement: function (error, element) {
                let container = element;
                if (element.closest('.input-group').length > 0) {
                    if ($(element).prev('.input-group-prepend').length > 0) {
                        container = $(element);
                    }
                    else {
                        container = $(element).closest('.input-group');
                    }
                }
                else if (element.hasClass('selectpicker') || element.hasClass('dropdown-select')) {
                    container = element.closest('.bootstrap-select');
                }
                else if (element.closest('button.dropdown-toggle').length > 0) {
                    container = element.closest('button.dropdown-toggle');
                }
                error.insertAfter(container);
            },
            highlight: function (element) {
                let container = $(element);
                if ($(element).closest('.input-group').length > 0) {
                    if ($(element).prev('.input-group-prepend').length > 0) {
                        container = $(element);
                    }
                    else if ($(element).next('.input-group-append').length > 0) {
                        container = $(element);
                    }
                    else {
                        container = $(element).closest('.input-group');
                    }
                }
                else if ($(element).hasClass('selectpicker') || $(element).hasClass('dropdown-select')) {
                    container = $(element).closest('.bootstrap-select');
                }
                else if ($(element).closest('button.dropdown-toggle').length > 0) {
                    container = $(element).closest('button.dropdown-toggle');
                }
                container.addClass("is-invalid");
            },
            unhighlight: function (element) {
                let container = $(element);
                if ($(element).closest('.input-group').length > 0) {
                    if ($(element).prev('.input-group-prepend').length > 0) {
                        container = $(element);
                    }
                    else if ($(element).next('.input-group-append').length > 0) {
                        container = $(element);
                    }
                    else {
                        container = $(element).closest('.input-group');
                    }
                }
                else if ($(element).hasClass('selectpicker') || $(element).hasClass('dropdown-select')) {
                    container = $(element).closest('.bootstrap-select');
                }
                else if ($(element).closest('button.dropdown-toggle').length > 0) {
                    container = $(element).closest('button.dropdown-toggle');
                }
                container.removeClass("is-invalid");
            },
            success: function (error) {
                $(error).remove();
            },
            onkeyup: function () {
                return false;
            },
            onfocusout: function () {
                return false;
            }
        });
    }
}

/**
 * ############################ # Base event binding Functions ############################
 */

function disableFormSubmitEvent() {
    $('form').on('keyup keypress', function (e) {
        let keyCode = e.keyCode || e.which;
        if (e.target.localName !== 'textarea' && keyCode === 13) {
            e.preventDefault();
            return false;
        }
    });
}

function bindRemoveButtonEvent(selector) {

    if (!isNotEmpty(selector)) {
        selector = ".remove";
    }

    $(document).off('click.bindRemove', selector).on('click.bindRemove', selector, function (e) {
        e.preventDefault();
        let url = $(this).attr("href");
        $("#modal-confirm-delete").modal({
            backdrop: 'static',
            keyboard: false
        });
        $("#btn-confirm-delete").off('click').on('click', function (e) {
            $("#modal-confirm-delete").modal("hide");
            post_to_url(url, {}, '_self', 'post');
        });
    });
}

/**
 * ################################## # Global Functions ##################################
 */

function clearOldValidationErrorMessages() {
    $(".invalid-feedback").remove();
    $('.form-control').removeClass('is-invalid');
}

function loadValidationErrors() {

    if (!window.APP_VALIDATION_ERRORS) {
        return;
    }

    const errors = window.APP_VALIDATION_ERRORS;

    if (Object.keys(errors).length === 0) {
        return;
    }

    clearOldValidationErrorMessages();

    $.each(errors, function (field, message) {

        // Select element by name attribute
        let $element = $('[name="' + field + '"]');

        if (!$element.length) {
            return;
        }

        let $container = $element;

        // Handle Bootstrap select / custom dropdowns
        if ($element.hasClass('selectpicker') || $element.hasClass('dropdown-select')) {
            $container = $element.closest('.bootstrap-select');
        }

        $element.addClass('is-invalid');

        $container.after(
            '<div class="invalid-feedback">' + message + '</div>'
        );
    });
}

function notify(title, message, style) {
    if (typeof toastr !== 'undefined') {
        switch (style) {
            case 'warning' :
                toastr.warning(message, title);
                break;
            case 'info' :
                toastr.info(message, title);
                break;
            case 'error' :
                toastr.error(message, title);
                break;
            case  'success' :
                toastr.success(message, title);
                break;
            default :
                toastr.error(message, title);
                break;

        }
    }
}

function handleServerResponse(response) {
    if (response.status === "METHOD_NOT_ALLOWED") {
        if (response.type === "validationError") {
            $("#validationErrors").empty();
            $.each(response.fieldErrors, (key, value) => {
                $("#validationErrors").append('<span class="error-item" data-id="' + key + '" data-error-message="' + value + '" />');
            });
            loadValidationErrors();
        }
    }
    else if (response.status === "OK") {
        $(".modal").modal("hide");
        clearOldValidationErrorMessages();
    }
    if (response.pageMessage) {
        let pageMessage = response.pageMessage;
        notify(pageMessage.title, pageMessage.message, pageMessage.style);
    }
}

function getLocalStorageItem(name) {
    if (typeof (Storage) !== 'undefined') {
        return localStorage.getItem(name);
    }
    else {
        window.alert('Please use a modern browser to properly view this template!');
    }
}

function saveInLocalStorage(name, val) {
    if (typeof (Storage) !== 'undefined') {
        localStorage.setItem(name, val);
    }
    else {
        window.alert('Please use a modern browser to properly view this template!');
    }
}

function removeFromLocalStorage(name) {
    if (typeof (Storage) !== 'undefined') {
        localStorage.removeItem(name);
    }
    else {
        window.alert('Please use a modern browser to properly view this template!');
    }
}

function goToHomePage() {
    if ($(".breadcrumb > li > a")[1]) {
        $(".breadcrumb > li > a")[1].click();
    }
    else {
        $(".breadcrumb > li > a")[0].click();
    }
}

function reloadCurrentPage() {
    location.reload(true);
}

/**
 * Get the context path (exclude "/").
 */
function getContextPath() {
    const contextPathMeta = $("meta[name='_ctx']").attr("content");
    if (contextPathMeta !== undefined && contextPathMeta !== null) {
        const normalizedContextPath = String(contextPathMeta).trim();
        if (normalizedContextPath === "" || normalizedContextPath === "/") {
            return "";
        }
        return normalizedContextPath.replace(/\/+$/, "");
    }
    return "";
}

function getApiResourcePath() {
    return getContextPath() + "/api/web/";
}

function sanitizeClientRequestToken(value) {
    const token = String(value == null ? '' : value).trim().toLowerCase();
    if (token === '') {
        return 'na';
    }
    return token.replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '') || 'na';
}

function buildTimeBucketedIdempotencyKey(prefix, keyParts = [], windowSeconds = 120) {
    const normalizedPrefix = sanitizeClientRequestToken(prefix);
    const bucketSeconds = Number.isInteger(windowSeconds) && windowSeconds > 0 ? windowSeconds : 120;
    const bucket = Math.floor(Date.now() / (bucketSeconds * 1000));
    const normalizedParts = Array.isArray(keyParts)
        ? keyParts.map(part => sanitizeClientRequestToken(part))
        : [];
    return [normalizedPrefix].concat(normalizedParts).concat(String(bucket)).join(':');
}

function resolveApiErrorTitle(xhr, fallbackTitle = 'Error') {
    if (handleUnauthorizedApiResponse(xhr, false)) {
        return "Session Expired";
    }
    if (xhr && xhr.responseJSON) {
        if (isNotEmpty(xhr.responseJSON.title)) {
            return xhr.responseJSON.title;
        }
        if (isNotEmpty(xhr.responseJSON.error)) {
            return xhr.responseJSON.error;
        }
    }
    return fallbackTitle;
}

function resolveApiErrorMessage(xhr, fallbackMessage = 'Something went wrong with requesting data to server.') {
    if (handleUnauthorizedApiResponse(xhr, false)) {
        return "Your session has expired. Redirecting to sign in.";
    }
    if (xhr && xhr.responseJSON) {
        if (isNotEmpty(xhr.responseJSON.message)) {
            return xhr.responseJSON.message;
        }
        if (isNotEmpty(xhr.responseJSON.detail)) {
            return xhr.responseJSON.detail;
        }
    }
    if (xhr && isNotEmpty(xhr.responseText)) {
        return xhr.responseText;
    }
    return fallbackMessage;
}

function handleUnauthorizedApiResponse(xhr, notifyUser = false) {
    if (!isUnauthorizedApiResponse(xhr)) {
        return false;
    }

    if (notifyUser) {
        notify("Session Expired", "Your session has expired. Redirecting to sign in.", "warning");
    }

    redirectToLoginIfNeeded();
    return true;
}

function applyDefaultAjaxHeaders(xhr, settings) {
    const csrfToken = $("meta[name='_csrf']").attr("content");
    const csrfHeader = $("meta[name='_csrf_header']").attr("content");
    const requestHeaders = settings && settings.headers ? settings.headers : {};
    const hasExplicitAcceptHeader = Object.keys(requestHeaders).some(function (name) {
        return String(name || "").toLowerCase() === "accept";
    });

    if (csrfToken && csrfHeader) {
        xhr.setRequestHeader(csrfHeader, csrfToken);
    }

    xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
    if (!hasExplicitAcceptHeader) {
        xhr.setRequestHeader("Accept", "application/json");
    }
}

function isUnauthorizedApiResponse(xhr) {
    const statusCode = Number(xhr && xhr.status ? xhr.status : 0);
    return statusCode === 401;
}

function redirectToLoginIfNeeded() {
    if (window.PROJECTX_AUTH_REDIRECT_IN_PROGRESS === true) {
        return;
    }

    window.PROJECTX_AUTH_REDIRECT_IN_PROGRESS = true;
    window.setTimeout(function () {
        window.location.href = getContextPath() + "/web/pub/login";
    }, 250);
}

function getPageMode() {
    return $("#pageMode").val();
}

function hasAuthority(actionName) {
    const permissions = Array.isArray(window.APP_PERMISSIONS) ? window.APP_PERMISSIONS : [];
    if (permissions.length === 0 || !isNotEmpty(actionName)) {
        return false;
    }

    const normalizedAction = String(actionName).trim();
    if (normalizedAction.length === 0) {
        return false;
    }

    // Super-admin and broad grants from backend.
    if (permissions.includes("*")) {
        return true;
    }

    if (permissions.includes(normalizedAction)) {
        return true;
    }

    // Support wildcard-style permission tokens, e.g. "topup.*".
    return permissions.some(permission => {
        if (!isNotEmpty(permission) || permission === "*") {
            return false;
        }
        const token = String(permission).trim();
        if (!token.endsWith(".*")) {
            return false;
        }
        const prefix = token.slice(0, -1);
        return normalizedAction.startsWith(prefix);
    });
}

function hasAnyAuthority(...actions) {
    return actions.some(hasAuthority);
}

function hasAllAuthorities(...actions) {
    return actions.every(hasAuthority);
}

function convertJSONValueToCommaSeparateString(acceptanceElemSelector) {
    try {
        let json = JSON.parse($(acceptanceElemSelector).val());
        $(acceptanceElemSelector).val(json);
    }
    catch (exception) {
    }
}

function getArrayValue(acceptanceElemSelector) {
    try {
        return JSON.parse($(acceptanceElemSelector).val());
    }
    catch (exception) {
    }
}

function removeElementByIndex(arr, x) {
    let newArr = [];
    for (let i = 0; i < arr.length; i++) {
        if (i !== x) {
            newArr.push(arr[i]);
        }
    }
    return newArr;
}

function formatNumber(x, maximumFractionDigits = 2) {
    if (x !== null && x !== undefined) {
        return x.toLocaleString(undefined, {maximumFractionDigits});
    }
    return "-";
}

function escapeHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function readCookie(name) {
    let nameEQ = name + "=";
    let ca = document.cookie.split(';');
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) === ' ') {
            c = c.substring(1, c.length);
        }
        if (c.indexOf(nameEQ) === 0) {
            return c.substring(nameEQ.length, c.length);
        }
    }
    return null;
}

function isNotEmpty(obj) {
    if (null == obj) {
        return false;
    }
    else if (obj.toString() !== "") {
        return true;
    }
    return false;
}

function getDataTablePageNumberFromUrl() {
    const pageParam = new URLSearchParams(window.location.search).get('page');
    const pageNumber = Number(pageParam);

    if (!Number.isInteger(pageNumber) || pageNumber < 1) {
        return 1;
    }
    return pageNumber;
}

function syncDataTablePageQueryParam(pageNumber) {
    if (!window.history || !window.history.replaceState) {
        return;
    }

    const parsedPageNumber = Number(pageNumber);
    if (!Number.isInteger(parsedPageNumber) || parsedPageNumber < 1) {
        return;
    }

    const url = new URL(window.location.href);
    url.searchParams.set('page', String(parsedPageNumber));

    const nextUrl = url.pathname + url.search + url.hash;
    const currentUrl = window.location.pathname + window.location.search + window.location.hash;
    if (nextUrl !== currentUrl) {
        window.history.replaceState({}, '', nextUrl);
    }
}

function isValidNRC(regionCode, townshipCode, nrcType, nrcNo) {
    return isNotEmpty(regionCode) && isNotEmpty(townshipCode) && isNotEmpty(nrcType) && isNotEmpty(nrcNo);
}

function isEmpty(obj) {
    return !isNotEmpty(obj);
}

function post_to_url(path, params, target, method) {
    method = method || "post";

    let form = document.createElement("form");
    form.setAttribute("method", method);
    form.setAttribute("action", path);
    form.setAttribute("target", target);

    let $csrf_token = $("meta[name='_csrf']").attr("content");
    let hiddenField = document.createElement("input");
    hiddenField.setAttribute("type", "hidden");
    hiddenField.setAttribute("name", "_csrf");
    hiddenField.setAttribute("value", $csrf_token);
    form.appendChild(hiddenField);

    for (let key in params) {
        if (params.hasOwnProperty(key)) {
            let hiddenField = document.createElement("input");
            hiddenField.setAttribute("type", "hidden");
            hiddenField.setAttribute("name", key);
            hiddenField.setAttribute("value", params[key]);
            form.appendChild(hiddenField);
        }
    }

    document.body.appendChild(form);
    form.submit();
}

/**
 * ####################################### # JavaScript utility methods for Array #######################################
 */

function convertUniqueArray(a) {
    let temp = {};
    for (let i = 0; i < a.length; i++) {
        temp[a[i]] = true;
    }
    let r = [];
    for (let k in temp)
        r.push(k);
    return r;
}

function findWithAttr(array, attr, value) {
    for (let i = 0; i < array.length; i += 1) {
        if (array[i][attr] === value) {
            return i;
        }
    }
    return -1;
}

String.prototype.replaceSome = function () {
    let replaceWith = Array.prototype.pop.apply(arguments), i = 0, r = this, l = arguments.length;
    for (; i < l; i++) {
        r = r.replace(arguments[i], replaceWith);
    }
    return r;
};

const generateUUID = () => {
    let
        d = new Date().getTime(),
        d2 = ((typeof performance !== 'undefined') && performance.now && (performance.now() * 1000)) || 0;
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        let r = Math.random() * 16;
        if (d > 0) {
            r = (d + r) % 16 | 0;
            d = Math.floor(d / 16);
        }
        else {
            r = (d2 + r) % 16 | 0;
            d2 = Math.floor(d2 / 16);
        }
        return (c === 'x' ? r : (r & 0x7 | 0x8)).toString(16);
    });
};

/* =========================================================
 *  Bootstrap-select SAFE helpers
 * ========================================================= */

function applySelectPickerValue($select, value) {
    if (!value) return;

    setTimeout(() => {

        // Remove bootstrap placeholder option if exists
        $select.find('option.bs-title-option').remove();

        // Set value via bootstrap-select ONLY
        $select.selectpicker('val', value);

        // Force UI sync
        $select.selectpicker('refresh');

        // Notify listeners correctly
        $select.trigger('changed.bs.select');

    }, 0);
}

function resetSelectPicker($select, disabled = true) {
    $select
        .selectpicker('val', '')
        .prop('disabled', disabled)
        .selectpicker('refresh');
}

function disableSelectPicker($select) {
    $select.prop('disabled', true).selectpicker('refresh');
}

function enableSelectPicker($select) {
    $select.prop('disabled', false).selectpicker('refresh');
}

function buildOptions(list, builderFn, includeEmpty = true) {
    const options = [];
    if (includeEmpty) {
        options.push('<option value="">Not selected</option>');
    }
    $.each(list, (_, item) => options.push(builderFn(item)));
    return options;
}

/* ----------------------------------------------------------------------------
    CSS: create CSS-Defintions dynamically
    's' can be
     - a string CSS(".myClass { color: red; }")
     - a template with multiple rules
        CSS(`
          .myClass { color: red; }
          .myClass:hover { color: green }
        `)
----------------------------------------------------------------------------*/
function CSS(s) {
    let rule = "", rules = [], sheet = window.document.styleSheets[0]  // Get styleSheet
    s.split("}").forEach(s => {
        let r = (s + "}").replace(/\r?\n|\r/g, "");  // create full rule again
        rule = (rule === "") ? r : rule + r
        if (rule.split('{').length === rule.split('}').length) { // equal number of brackets?
            sheet.insertRule(rule, sheet.cssRules.length)
            rule = ""
        }
    })
}

function isCommonApiResponseSuccess(response) {
    if (!response) {
        return false;
    }

    if (response.statusCode !== undefined && response.statusCode !== null) {
        return Number(response.statusCode) >= 200 && Number(response.statusCode) < 300;
    }

    if (response.status !== undefined && response.status !== null) {
        const status = String(response.status).toUpperCase();
        return status === "OK" || status === "SUCCESS" || status === "VALIDATED";
    }

    return true;
}

function notifyCommonApiResponse(response, defaultSuccessMessage = "Request finished.", defaultErrorMessage = "Request failed.") {
    const ok = isCommonApiResponseSuccess(response);
    const title = response?.title || (ok ? "Success !" : "Error !");
    const message = response?.message || (ok ? defaultSuccessMessage : defaultErrorMessage);
    notify(title, message, ok ? "success" : "error");
    return ok;
}
