/*
 * ====================================================================== 
 * dataTables-select.js ! version : 1.0 Copyright (c) 2017 - license.txt, Than Htike Aung
 * ======================================================================
 */

/**
 * @file dataTables-select.js
 * @description Make JQuery DataTable selectable.
 * @version 1.0
 * @author Than Htike Aung
 * @contact rage.cataclysm@gmail.com
 * @copyright Copyright (c) 2017-2018, Than Htike Aung. This source file is free software, available under the following license: MIT license. See the license file for details.
 */

let eventFromSelectAll = false;

/*
 * @param tableSelector {string} pass table selector
* @param acceptanceElemSelector {string} you must define an element this can accept for value insert. <input type="hidden"> is preferable.
 */
function setSelectable(tableSelector, acceptanceElemSelector, itemCounts) {
    if (!$(tableSelector).hasClass("selectable")) {
        setup(tableSelector);
    }
    // no records ? no selectable
    let selectAllCheckBox = tableSelector + ' thead .pretty input';
    if (itemCounts === 0) {
        eventFromSelectAll = true;
        $(selectAllCheckBox).prop("checked", false).trigger('change');
        eventFromSelectAll = false;
        $(selectAllCheckBox).attr("disabled", true);
        return false;
    }
    $(selectAllCheckBox).attr("disabled", false);
    setDefaultSelect(tableSelector, acceptanceElemSelector);
    bindSelectRowEvent(tableSelector, acceptanceElemSelector);
}

// bind events for table select rows
function bindSelectRowEvent(tableSelector, acceptanceElemSelector) {
    let selectAllCheckBox = tableSelector + ' thead .pretty input';
    let selectRowCheckBoxes = tableSelector + ' tbody tr .pretty input';
    // write the code for single select row event
    // when checkbox was clicked, make associated table row as selected style or not
    $(selectRowCheckBoxes).on('custom-toggled', function (e) {
        if ($(this).prop('checked')) {
            $(this).closest("tr").addClass("selected");
        }
        else {
            $(this).closest("tr").removeClass("selected");
        }

        if (eventFromSelectAll === true) {
            return false;
        }
        // determine select all checkbox should checked or not
        let totalItemCounts = getTotalItemCounts(tableSelector);
        let selectedItemCounts = getSelectedCounts(tableSelector);
        eventFromSelectAll = true;
        if (selectedItemCounts === totalItemCounts) {
            $(selectAllCheckBox).prop("checked", true);
        }
        else {
            $(selectAllCheckBox).prop("checked", false);
        }
        eventFromSelectAll = false;
        // in my case, dataId is Integer type
        let dataId = parseInt($(this).attr("data-id"));
        // record selected Item
        recordSelectedItemBySingleSelect(tableSelector, acceptanceElemSelector, dataId, $(this).prop('checked'));
    });

    // bind select all,de-select all events
    $(selectAllCheckBox).on('change', function (e) {
        if (!eventFromSelectAll) {
            eventFromSelectAll = true;
            if ($(this).prop('checked')) {
                $(selectRowCheckBoxes).prop("checked", true).trigger('custom-toggled');
            }
            else {
                $(selectRowCheckBoxes).prop("checked", false).trigger('custom-toggled');
            }
            eventFromSelectAll = false;
            // record selected Item
            recordSelectedItemBySelectAll(tableSelector, acceptanceElemSelector, $(this).prop('checked'));
        }
    });

    // when table's row was clicked, make toggle on the checkbox also
    // but don't write the code for select row event here
    $(tableSelector + " tbody tr").on("click", function (e) {
        if (!$(e.target).hasClass('details-control')) {
            let checkbox = $(this).find('.pretty input');
            if (e.target.nodeName !== 'INPUT') {
                checkbox.prop("checked", !checkbox.prop("checked"));
            }
            checkbox.trigger('custom-toggled');
        }
    });
}

// to show the pre-selected elements
function setDefaultSelect(tableSelector, acceptanceElemSelector) {
    // uncheck selectall checkbox first for page navigation
    let selectAllCheckBox = tableSelector + ' thead .pretty input';
    $(selectAllCheckBox).prop("checked", false);

    let selectedItems = [];
    let originalValue = $(acceptanceElemSelector).val();
    if (originalValue && originalValue.length > 0) {
        if (!originalValue.startsWith('[')) {
            originalValue = "[" + originalValue + "]";
        }
        selectedItems = JSON.parse(originalValue);
        $.each(selectedItems, function (e, dataId) {
            let checkBox = $(tableSelector + ' tbody .pretty input[data-id="' + dataId + '"]');
            $(checkBox).prop("checked", true).trigger('custom-toggled');
            $(checkBox).closest("tr").addClass("selected");
        });

        // determine select all checkbox should checked or not
        let totalItemCounts = getTotalItemCounts(tableSelector);
        let selectedItemCounts = getSelectedCounts(tableSelector);
        if (selectedItemCounts === totalItemCounts) {
            $(selectAllCheckBox).prop("checked", true).trigger('change');
        }

        $(acceptanceElemSelector).val(JSON.stringify(selectedItems));
        // show how many items are selected
        showSelectedCounts(tableSelector, selectedItems.length);
    }
    else {
        // hide
        $(tableSelector).parent().find(".itemCountWrapper").hide();
    }
}

function getTotalItemCounts(tableSelector) {
    return $(tableSelector + ' tbody tr .pretty input').length;
}

function getSelectedCounts(tableSelector) {
    return $(tableSelector + ' tbody tr .pretty input:checked').length;
}

function showSelectedCounts(tableSelector, count) {
    let itemCountElem = $(tableSelector).parent().find(".itemCountWrapper");
    $(itemCountElem).show();
    $(itemCountElem).find(".count").text(count);
    if (count > 0) {
        $(itemCountElem).animate({
            "opacity": "1",
            top: "3px"
        }, 300);
    }
    else {
        $(itemCountElem).animate({
            "opacity": "0",
            top: "20px"
        }, 300);
    }
}

// for single selected row
function recordSelectedItemBySingleSelect(tableSelector, acceptanceElemSelector, dataId, isChecked) {
    let selectedItems = [];
    if ($(acceptanceElemSelector).val() && $(acceptanceElemSelector).val().length > 0) {
        selectedItems = JSON.parse($(acceptanceElemSelector).val());
    }
    let index = selectedItems.indexOf(dataId);
    if (index > -1) {
        selectedItems.splice(index, 1);
    }
    if (isChecked) {
        selectedItems.push(dataId);
    }
    $(acceptanceElemSelector).val(JSON.stringify(selectedItems));
    // show how many items are selected
    showSelectedCounts(tableSelector, selectedItems.length);
}

// for select all or de-select all
function recordSelectedItemBySelectAll(tableSelector, acceptanceElemSelector, isChecked) {
    let selectedItems = [];
    // put back items from previous pages
    let originalValue = $(acceptanceElemSelector).val();
    if (originalValue && originalValue.length > 0) {
        if (!originalValue.startsWith('[')) {
            originalValue = "[" + originalValue + "]";
        }
        selectedItems = JSON.parse(originalValue);
    }

    let selectRowCheckBoxes = $(tableSelector + ' tbody tr .pretty input');
    $.each(selectRowCheckBoxes, function (e, elem) {
        // in my case, dataId is Integer type
        let dataId = parseInt($(elem).attr("data-id"));
        let index = selectedItems.indexOf(dataId);
        if (index > -1) {
            selectedItems.splice(index, 1);
        }
        if (isChecked) {
            selectedItems.push(dataId);
        }
    });
    $(acceptanceElemSelector).val(JSON.stringify(selectedItems));
    // show how many items are selected
    showSelectedCounts(tableSelector, selectedItems.length);
}

// setup for dataTable select
function setup(tableSelector) {
    // add selectable class
    $(tableSelector).addClass("selectable");

    // this element will show selected item counts
    let itemCountElem = $('<div class="itemCountWrapper text-center"><a>Total ( <span class="count">0</span> ) selected</a></div>');
    $(tableSelector).before(itemCountElem);
}

// fetch selected items from selectable dataTable
function getSelectedItems(acceptanceElemSelector) {
    if ($(acceptanceElemSelector).val() && $(acceptanceElemSelector).val().length > 0) {
        return JSON.parse($(acceptanceElemSelector).val());
    }
    else {
        return "";
    }
}

function clearSelectedItems(tableSelector, acceptanceElemSelector) {
    $(tableSelector + "_selectedItems").val('');
    $(acceptanceElemSelector).val('');
    showSelectedCounts(tableSelector, 0);
}