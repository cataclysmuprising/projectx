/*
 * ======================================================================
 * dataTables-select.js ! version : 1.1 (DT 2.x + scrollX safe)
 * Copyright (c) 2017, Than Htike Aung
 * License: MIT
 *
 * Rewrite goals:
 * - No custom events needed (use native `change`)
 * - Works when user clicks checkbox OR clicks row (optional)
 * - DT 2.x + scrollX safe: header selection via api.table().header()
 * - Safe rebinding (namespaced events)
 * ======================================================================
 */

(function () {

    let internalBatchUpdating = false;

    /* -------------------------------------------------------
     * Helpers (DT 2.x scrollX safe)
     * ----------------------------------------------------- */

    function getApi(tableSelector) {
        return $(tableSelector).DataTable();
    }

    function getSelectAllCheckbox(tableSelector) {
        const api = getApi(tableSelector);
        return $(api.table().header()).find('.pretty input[type="checkbox"]');
    }

    function getSelectAllPretty(tableSelector) {
        const api = getApi(tableSelector);
        return $(api.table().header()).find('.pretty');
    }

    function getRowCheckboxSelector(tableSelector) {
        return tableSelector + ' tbody tr .pretty input[type="checkbox"]';
    }

    function normalizeSelectedValue(raw) {
        if (!raw) return [];
        if (!raw.startsWith('[')) raw = "[" + raw + "]";
        try {
            const parsed = JSON.parse(raw);
            return Array.isArray(parsed) ? parsed : [];
        }
        catch (e) {
            return [];
        }
    }

    function uniqueIntArray(arr) {
        const set = new Set();
        (arr || []).forEach(v => {
            const n = parseInt(v, 10);
            if (!Number.isNaN(n)) set.add(n);
        });
        return Array.from(set);
    }

    /* -------------------------------------------------------
     * Public API
     * ----------------------------------------------------- */

    /**
     * @param tableSelector {string}
     * @param acceptanceElemSelector {string} hidden input that stores [id,id,...]
     * @param itemCounts {number}
     * @param options {object|boolean} if boolean -> enableRowClickSelect
     */
    window.setSelectable = function (tableSelector, acceptanceElemSelector, itemCounts, options = {}) {

        // backward compatible: 4th param boolean
        if (typeof options === 'boolean') {
            options = {enableRowClickSelect: options};
        }

        const settings = {
            enableRowClickSelect: true,
            ignoreRowClickSelectors: ['.details-control', 'a', 'button', 'input', 'select', 'textarea', 'label'],
            ...options
        };

        if (!$(tableSelector).hasClass("selectable")) {
            setup(tableSelector, settings);
        }

        // ✅ cursor pointer control
        if (settings.enableRowClickSelect) {
            $(tableSelector).addClass("row-click-selectable");
        }
        else {
            $(tableSelector).removeClass("row-click-selectable");
        }

        const $selectAllCheckBox = getSelectAllCheckbox(tableSelector);

        // no records ? disable select-all + clear UI
        if (itemCounts === 0) {
            internalBatchUpdating = true;
            $selectAllCheckBox.prop("checked", false);
            internalBatchUpdating = false;
            $selectAllCheckBox.prop("disabled", true);
            showSelectedCounts(tableSelector, 0);
            return false;
        }

        $selectAllCheckBox.prop("disabled", false);

        // bind events first so preloaded selections can also paint row state
        bindSelectRowEvent(tableSelector, acceptanceElemSelector, settings);

        // then apply default selection from acceptance field
        applyDefaultSelection(tableSelector, acceptanceElemSelector);

        // update select-all state just in case
        refreshSelectAllState(tableSelector, $selectAllCheckBox);
        return true;
    };

    window.getSelectedItems = function (acceptanceElemSelector) {
        return normalizeSelectedValue($(acceptanceElemSelector).val());
    };

    window.clearSelectedItems = function (tableSelector, acceptanceElemSelector) {
        $(acceptanceElemSelector).val('');
        showSelectedCounts(tableSelector, 0);

        const $selectAll = getSelectAllCheckbox(tableSelector);

        internalBatchUpdating = true;
        $(getRowCheckboxSelector(tableSelector)).prop('checked', false).trigger('change');
        $selectAll.prop('checked', false);
        internalBatchUpdating = false;
    };

    /* -------------------------------------------------------
     * Bind events
     * ----------------------------------------------------- */

    function bindSelectRowEvent(tableSelector, acceptanceElemSelector, settings) {

        const $selectAllCheckBox = getSelectAllCheckbox(tableSelector);
        const $selectAllPretty = getSelectAllPretty(tableSelector);
        const rowCheckboxSelector = getRowCheckboxSelector(tableSelector);

        /* -------------------------------
         * 1) Single row checkbox changed
         * ----------------------------- */
        $(document)
            .off('change.dtSelectable', rowCheckboxSelector)
            .on('change.dtSelectable', rowCheckboxSelector, function () {

                const $cb = $(this);
                const isChecked = $cb.prop('checked');

                $cb.closest('tr').toggleClass('selected', isChecked);

                // update select-all checkbox visual state
                refreshSelectAllState(tableSelector, $selectAllCheckBox);

                // record to acceptance field ONLY once per user action
                // during internal batch we don't do per-row write
                if (!internalBatchUpdating) {
                    const dataId = parseInt($cb.attr('data-id'), 10);
                    recordSelectedItemBySingleSelect(tableSelector, acceptanceElemSelector, dataId, isChecked);
                }
            });

        /* --------------------------------------------------
         * 2) SELECT-ALL CLICK BRIDGE (DT 2.x + Pretty)
         * ------------------------------------------------ */
        // Some "pretty" checkbox libs cause header sort by click bubbling.
        $selectAllPretty
            .off('mousedown.dtSelectableSelectAll click.dtSelectableSelectAll')
            .on('mousedown.dtSelectableSelectAll', function (e) {
                e.preventDefault();
                e.stopPropagation();
            })
            .on('click.dtSelectableSelectAll', function (e) {
                if (e.target && e.target.nodeName === 'INPUT') return;

                e.preventDefault();
                e.stopPropagation();

                if ($selectAllCheckBox.is(':disabled')) return;

                $selectAllCheckBox
                    .prop('checked', !$selectAllCheckBox.prop('checked'))
                    .trigger('change');
            });

        /* -------------------------------
         * 3) Select all / deselect all
         * ----------------------------- */
        $selectAllCheckBox
            .off('change.dtSelectableSelectAll')
            .on('change.dtSelectableSelectAll', function () {

                if (internalBatchUpdating) return;

                const isChecked = $(this).prop('checked');

                internalBatchUpdating = true;
                $(rowCheckboxSelector).prop('checked', isChecked).trigger('change');
                internalBatchUpdating = false;

                // one write for select-all
                recordSelectedItemBySelectAll(tableSelector, acceptanceElemSelector, isChecked);
            });

        /* -------------------------------
         * 4) Row click toggles checkbox (OPTIONAL)
         * ----------------------------- */
        $(document).off('click.dtSelectableRow', tableSelector + ' tbody tr');

        if (settings.enableRowClickSelect) {

            $(document).on('click.dtSelectableRow', tableSelector + ' tbody tr', function (e) {

                // ignore clicks on interactive elements / details-control
                const ignore = settings.ignoreRowClickSelectors || [];
                for (let i = 0; i < ignore.length; i++) {
                    if ($(e.target).closest(ignore[i]).length) return;
                }

                const $cb = $(this).find('.pretty input[type="checkbox"]').first();
                if ($cb.length === 0) return;

                $cb.prop('checked', !$cb.prop('checked')).trigger('change');
            });
        }
    }

    /* -------------------------------------------------------
     * Default select
     * ----------------------------------------------------- */

    function applyDefaultSelection(tableSelector, acceptanceElemSelector) {

        const $selectAllCheckBox = getSelectAllCheckbox(tableSelector);
        $selectAllCheckBox.prop("checked", false);

        const selectedItems = uniqueIntArray(normalizeSelectedValue($(acceptanceElemSelector).val()));

        if (selectedItems.length === 0) {
            $(tableSelector).parent().find(".itemCountWrapper").hide();
            return;
        }

        // batch apply: check needed items, let `change` update row UI
        internalBatchUpdating = true;

        selectedItems.forEach(function (dataId) {
            const $cb = $(tableSelector + ' tbody .pretty input[type="checkbox"][data-id="' + dataId + '"]');
            $cb.prop("checked", true).trigger('change');
        });

        internalBatchUpdating = false;

        syncRowSelectionState(tableSelector);

        // store normalized unique array
        $(acceptanceElemSelector).val(JSON.stringify(selectedItems));
        showSelectedCounts(tableSelector, selectedItems.length);

        refreshSelectAllState(tableSelector, $selectAllCheckBox);
    }

    function refreshSelectAllState(tableSelector, $selectAllCheckBox) {
        const total = getTotalItemCounts(tableSelector);
        const selected = getSelectedCounts(tableSelector);

        internalBatchUpdating = true;
        $selectAllCheckBox.prop("checked", total > 0 && selected === total);
        internalBatchUpdating = false;
    }

    function syncRowSelectionState(tableSelector) {
        $(tableSelector + ' tbody tr').each(function () {
            const $row = $(this);
            const isChecked = $row.find('.pretty input[type="checkbox"]').first().prop('checked') === true;
            $row.toggleClass('selected', isChecked);
        });
    }

    /* -------------------------------------------------------
     * Utility functions
     * ----------------------------------------------------- */

    function getTotalItemCounts(tableSelector) {
        return $(getRowCheckboxSelector(tableSelector)).length;
    }

    function getSelectedCounts(tableSelector) {
        return $(getRowCheckboxSelector(tableSelector) + ':checked').length;
    }

    function showSelectedCounts(tableSelector, count) {
        const $wrapper = $(tableSelector).parent().find(".itemCountWrapper");
        $wrapper.show();
        $wrapper.find(".count").text(count);

        $wrapper.animate(
            {opacity: count > 0 ? "1" : "0", top: count > 0 ? "3px" : "20px"},
            300
        );

        if (count <= 0) {
            // keep old behavior: hide wrapper when nothing selected
            // comment out if you want it always visible
            // $wrapper.hide();
        }
    }

    function recordSelectedItemBySingleSelect(tableSelector, acceptanceElemSelector, dataId, isChecked) {
        if (Number.isNaN(dataId)) return;

        let selectedItems = normalizeSelectedValue($(acceptanceElemSelector).val());
        selectedItems = uniqueIntArray(selectedItems);

        selectedItems = selectedItems.filter(v => v !== dataId);
        if (isChecked) selectedItems.push(dataId);

        selectedItems = uniqueIntArray(selectedItems);

        $(acceptanceElemSelector).val(JSON.stringify(selectedItems));
        showSelectedCounts(tableSelector, selectedItems.length);
    }

    function recordSelectedItemBySelectAll(tableSelector, acceptanceElemSelector, isChecked) {

        let selectedItems = normalizeSelectedValue($(acceptanceElemSelector).val());
        selectedItems = uniqueIntArray(selectedItems);

        const $rows = $(getRowCheckboxSelector(tableSelector));

        $rows.each(function () {
            const dataId = parseInt($(this).attr("data-id"), 10);
            if (Number.isNaN(dataId)) return;

            selectedItems = selectedItems.filter(v => v !== dataId);
            if (isChecked) selectedItems.push(dataId);
        });

        selectedItems = uniqueIntArray(selectedItems);

        $(acceptanceElemSelector).val(JSON.stringify(selectedItems));
        showSelectedCounts(tableSelector, selectedItems.length);
    }

    function setup(tableSelector, settings) {

        $(tableSelector).addClass("selectable");

        const $wrapper = $('<div class="itemCountWrapper text-center"></div>');

        const $label = $(
            '<span class="selectedLabel">Total ( <span class="count">0</span> ) selected</span>'
        );

        $wrapper.append($label);

        // actions
        if (settings.actions && Array.isArray(settings.actions)) {

            settings.actions.forEach(function (action) {

                if (action.showOnExpression !== undefined && action.showOnExpression !== null) {
                    if (action.showOnExpression === false) {
                        return; // skip this action
                    }
                }

                const $btn = $('<button type="button"></button>')
                    .addClass(action.class || 'btn btn-flat bg-gradient-primary');

                if (action.id) $btn.attr("id", action.id);

                // icon
                if (action.iconClass) {
                    $btn.append('<i class="' + action.iconClass + '"></i> ');
                }

                // text
                $btn.append(action.text || 'Action');

                // click callback
                if (action.onClick && typeof action.onClick === "function") {
                    $btn.on("click", function (e) {
                        e.preventDefault();
                        action.onClick(tableSelector);
                    });
                }

                $wrapper.append(" ");
                $wrapper.append($btn);
            });
        }

        $(tableSelector).before($wrapper);
    }

})();
