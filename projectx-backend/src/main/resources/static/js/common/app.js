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
$(document).ajaxSend((e, xhr, options) => {
    let $csrf_token = $("meta[name='_csrf']").attr("content");
    let $csrf_header = $("meta[name='_csrf_header']").attr("content");
    if ($csrf_token && $csrf_header) {
        xhr.setRequestHeader($csrf_header, $csrf_token);
    }
    xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
    xhr.setRequestHeader('Accept', 'application/json');
}).ajaxComplete((event, xhr, settings) => {
    // if (xhr.status === 208 || xhr.status === 226) {
    //     document.write(xhr.responseText);
    // }
}).ajaxError((event, xhr, settings, thrownError) => {
    notify("Error !", "Something went wrong with requesting data to server.", "error");
    console.log(xhr, thrownError);
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
        Object.assign(DataTable.defaults, {
            "lengthChange": false,
            "searching": false,
            pagingType: "first_last_numbers",
            "pageLength": ROW_PER_PAGE,
            processing: true,
            serverSide: true,
            autoWidth: false,
            aaSorting: [],
            "language": {
                "sProcessing": "&nbsp;",
                "sZeroRecords": "No matching records found.",
                "sEmptyTable": "No matching records found.",
                "sLoadingRecords": "&nbsp;",
            },
            infoCallback: function (roles, start, end, max, total, pre) {
                if (total > 0) {
                    return "Showing " + start + " to " + end + " of " + total + " records";
                }
                else {
                    return "Empty records";
                }
            }
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
}

function initSelectPickers() {
    // set selected value for select picker
    $(".selectpicker,.dropdown-select").on('loaded.bs.select', function (e) {
        let _selected = $(this).attr("data-selected");
        if (_selected && _selected.trim().length > 0) {
            // for multiple select picker
            if ($(this).attr("multiple")) {
                _selected = _selected.replaceSome("]", "[", " ", "");
                $(this).selectpicker('val', _selected.split(","));
            }

            else {
                $(this).selectpicker('val', _selected);
                //$(this).selectpicker('val', _selected).change();
            }
        }
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

    $(selector).on("click", function (e) {
        e.preventDefault();
        let url = $(this).attr("href");
        $("#modal-confirm-delete").modal({
            backdrop: 'static',
            keyboard: false
        });
        $("#btn-confirm-delete").off('click').on('click', function (e) {
            $("#modal-confirm-delete").modal("hide");
            window.location.href = url;
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
    let errorElems = $("#validationErrors .error-item");
    if (errorElems.length > 0) {
        clearOldValidationErrorMessages();
        $.each(errorElems, function (index, item) {
            let elementId = $(item).attr("data-id").replace(new RegExp("\\.", "g"), '_');
            let errorElem = $("#" + elementId);
            let container = errorElem;
            if (errorElem.hasClass('selectpicker') || errorElem.hasClass('dropdown-select')) {
                container = errorElem.closest('.bootstrap-select');
            }
            if (errorElem.length) {
                container.addClass('is-invalid');
                container.after('<div class="invalid-feedback">' + $(item).attr("data-error-message") + '</div>');
            }
        });
    }
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
    return window.location.pathname.substring(0, window.location.pathname.indexOf("/", 2));
}

function getApiResourcePath() {
    return getContextPath() + "/api/";
}

function getStaticResourcePath() {
    return $("#baseStaticRssDir").val();
}

function getPageMode() {
    return $("#pageMode").val();
}

function hasAuthority(actionName) {
    return $("#" + actionName).val() === "true";
}

function convertJSONValueToCommaSeparateString(acceptanceElemSelector) {
    try {
        let json = JSON.parse($(acceptanceElemSelector).val());
        $(acceptanceElemSelector).val(json);
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

function formatNumber(x) {
    if (x !== null && x !== undefined) {
        return x.toLocaleString(undefined, {maximumFractionDigits: 2});
    }
    return "-";
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

/**
 * Format the NRC including passport number.
 * @return The formatted NRC or the Passport number.
 */
function formatNRC(data) {
    return data.passportNo ? data.passportNo : data.nrcRegionCode + "/" + data.nrcTownshipCode + "(" + data.nrcType + ")" + data.nrcNo;
}

function setOptionsInSelect(targetElems, options, selectedValue) {
    $(targetElems).each(function (index, elem) {
        let oldSelectValue = $(elem).val();
        $(elem).html(options).selectpicker('refresh');
        // အကယ်၍ အရင်ကတစ်ခုခုရွေးထားပြီးသားရှိနေရင် dropdown item list ကို update လုပ်ပြီး အရင်ရွေးထားတဲ့ဟာကို select ပြန်လုပ်ပေးထားမယ်။
        if (isNotEmpty(oldSelectValue)) {
            $(elem).selectpicker('val', oldSelectValue);
        }
    });
    // အသစ်ထည့်လိုက်တဲ့ item ကို select လုပ်ပေးမယ်။
    if (selectedValue) $(targetElems).selectpicker('val', selectedValue);
}

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