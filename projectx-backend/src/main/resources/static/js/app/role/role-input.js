let tblAdministrator, tblAction;

function init() {
    initAdministratorTable();
    initActionTable();
    initValidator();
    loadModuleNames();
    loadAccessLevels();
}

function bind() {

    $("#btnCancel").on("click", function (e) {
        goToHomePage();
    });

    $("#btnSubmit").on("click", function (e) {
        convertJSONValueToCommaSeparateString("#administratorIds");
        convertJSONValueToCommaSeparateString("#actionIds");
        $("#roleForm").submit();
    });

    $("#btnReset").on("click", function (e) {
        reloadCurrentPage();
    });

    $("#btnAdministratorReset").on("click", function (e) {
        reloadCurrentPage();
    });

    $("#btnAdministratorSearch").on("click", function (e) {
        tblAdministrator.draw();
    });

    $("#btnActionReset").on("click", function (e) {
        reloadCurrentPage();
    });

    $("#btnActionSearch").on("click", function (e) {
        tblAction.draw();
    });

    $("#pageName").on("change", function (e) {
        tblAction.draw();
    });

    $("#accessLevel").on("change", function (e) {
        tblAction.draw();
    });

}

function initAdministratorTable() {
    let columns = [{
        "render": function (data, type, full, meta) {
            return '<div class="pretty p-default"><input data-id="' + full.id + '" type="checkbox"/><div class="state p-default-o"><label></label></div></div>';
        },
        "bSortable": false,
        "sClass": "text-center"
    }, {
        "mData": "name",
        "sClass": "text-left"
    }, {
        "mData": "loginId",
        "sClass": "text-left"
    }];
    tblAdministrator = $('#tblAdministrator').DataTable({
        aoColumns: columns,
        "aaSorting": [],
        "pageLength": SECONDARY_ROW_PER_PAGE,
        ajax: {
            type: "POST",
            url: getApiResourcePath() + 'sec/administrator/search/paging',
            data: function (d) {
                let criteria = buildSortCriteria(d, "#tblAdministrator");
                criteria.pageNumber = d.length > 0 ? Math.floor(d.start / d.length) + 1 : 1;
                criteria.limit = d.length;
                criteria.status = "ACTIVE";
                let word = $("#administrator-keyword").val();
                if (isNotEmpty(word)) {
                    criteria.keyword = word;
                }
                return JSON.stringify(criteria);
            }
        },
        initComplete: function () {
            let api = this.api();
            $('#administrator-keyword').off('.DT').on('keyup.DT', function (e) {
                if (e.keyCode === 13) {
                    api.search(this.value).draw();
                }
            });
        },
        drawCallback: function (settings) {
            setSelectable("#tblAdministrator", "#administratorIds", settings._iRecordsDisplay);
        }
    });
}

function initActionTable() {
    let columns = [{
        "render": function (data, type, full, meta) {
            return '<div class="pretty p-default p-pulse"><input data-id="' + full.id + '" type="checkbox"/><div class="state p-default-o"><label></label></div></div>';
        },
        "bSortable": false,
        "sClass": "text-center"
    }, {
        "render": function (data, type, full, meta) {
            return renderPlainText(full.page, type);
        },
        "sClass": "text-left"
    }, {
        "render": function (data, type, full, meta) {
            return renderPlainText(full.displayName, type);
        },
        "bSortable": false,
        "sClass": "text-left"
    }, {
        "render": function (data, type, full, meta) {
            return renderAccessLevel(full.accessLevel, type);
        },
        "sClass": "text-left role-access-column"
    }, {
        "render": function (data, type, full, meta) {
            return renderRouteCoverage(full, type);
        },
        "bSortable": false,
        "sClass": "text-left role-route-column"
    }, {
        "render": function (data, type, full, meta) {
            return renderPlainText(full.description, type);
        },
        "bSortable": false,
        "sClass": "text-left role-description-column"
    }];

    tblAction = $('#tblAction').DataTable({
        aoColumns: columns,
        "aaSorting": [],
        "pageLength": SECONDARY_ROW_PER_PAGE,
        ajax: {
            type: "POST",
            url: getApiResourcePath() + 'sec/action/search/paging',
            data: function (d) {
                let criteria = buildSortCriteria(d, "#tblAction");
                criteria.pageNumber = d.length > 0 ? Math.floor(d.start / d.length) + 1 : 1;
                criteria.limit = d.length;
                if (isNotEmpty($("#pageName").val())) {
                    criteria.page = $("#pageName").val();
                }
                if (isNotEmpty($("#accessLevel").val())) {
                    criteria.accessLevel = $("#accessLevel").val();
                }
                let word = $("#action-keyword").val();
                if (isNotEmpty(word)) {
                    criteria.keyword = word;
                }
                return JSON.stringify(criteria);
            }
        },
        initComplete: function () {
            let api = this.api();
            $('#action-keyword').off('.DT').on('keyup.DT', function (e) {
                if (e.keyCode === 13) {
                    api.search(this.value).draw();
                }
            });
        },
        drawCallback: function (settings) {
            setSelectable("#tblAction", "#actionIds", settings._iRecordsDisplay);
        }
    });
}

function loadModuleNames() {
    let criteria = {};
    $.ajax({
        type: "GET",
        url: getApiResourcePath() + 'sec/action/pages',
        data: criteria,
        success: function (data) {
            let options = [];
            options.push("<option data-srcode='' value=''>Module (All)</option>");
            $.each(data, function (key, item) {
                let option = "<option  value='" + item + "'>" + item + "</option>";
                options.push(option);
            });
            $("#pageName").html(options).selectpicker('refresh');
        }
    });
}

function loadAccessLevels() {
    const options = [
        "<option value=''>Access (All)</option>",
        "<option value='READ'>Read</option>",
        "<option value='WRITE'>Write</option>",
        "<option value='SENSITIVE'>Sensitive</option>"
    ];
    $("#accessLevel").html(options.join('')).selectpicker('refresh');
}

function initValidator() {
    $("#roleForm").validate({
        rules: {
            "name": {
                required: true,
                minlength: 3,
                maxlength: 20
            },
            "description": {
                required: true,
                maxlength: 200
            }
        },
        messages: {
            "name": {
                required: "'Role Name' should not be empty.",
                minlength: "'Role Name' should be at least 3 characters.",
                maxlength: "'Role Name' should not exceeds 20 characters."
            },
            "description": {
                required: "'Description' should not be empty.",
                maxlength: "'Description' should not exceeds 200 characters."
            }
        }
    });
}

function buildSortCriteria(dataTableRequest, tableSelector) {
    let criteria = {};
    if (!dataTableRequest.order || dataTableRequest.order.length === 0) {
        return criteria;
    }

    const order = dataTableRequest.order[0];
    const sortColumn = $(tableSelector + " thead th").eq(order.column);
    const sortKey = sortColumn.data("sort-key");
    if (isEmpty(sortKey)) {
        return criteria;
    }

    criteria.sortKeys = [sortKey];
    criteria.sortDirs = [String(order.dir || "asc").toUpperCase()];
    return criteria;
}

function renderRouteCoverage(action, type) {
    const primaryRoute = trimToNull(action.primaryRoute);
    const totalRouteCount = normalizeWholeNumber(action.totalRouteCount, primaryRoute ? 1 : 0);
    const pageSupportRouteCount = normalizeWholeNumber(action.pageSupportRouteCount, 0);
    const sharedLookupRouteCount = normalizeWholeNumber(action.sharedLookupRouteCount, 0);

    if (type !== 'display') {
        return [
            primaryRoute || '',
            'total ' + totalRouteCount,
            'page support ' + pageSupportRouteCount,
            'shared lookup ' + sharedLookupRouteCount
        ].join(' ').trim();
    }

    const badges = [];
    badges.push('<span class="badge badge-light role-route-badge">Total ' + formatNumber(totalRouteCount, 0) + '</span>');
    if (pageSupportRouteCount > 0) {
        badges.push('<span class="badge badge-info role-route-badge">Page Support ' + formatNumber(pageSupportRouteCount, 0) + '</span>');
    }
    if (sharedLookupRouteCount > 0) {
        badges.push('<span class="badge badge-warning role-route-badge">Shared Lookup ' + formatNumber(sharedLookupRouteCount, 0) + '</span>');
    }
    if (pageSupportRouteCount === 0 && sharedLookupRouteCount === 0 && totalRouteCount <= 1) {
        badges.push('<span class="badge badge-secondary role-route-badge">Direct Only</span>');
    }

    const primaryRouteHtml = primaryRoute
        ? '<div class="role-route-coverage__primary">' + escapeHtml(primaryRoute) + '</div>'
        : '<div class="role-route-coverage__empty">No runtime routes mapped.</div>';

    return '<div class="role-route-coverage">'
        + primaryRouteHtml
        + '<div class="role-route-coverage__meta">' + badges.join('') + '</div>'
        + '</div>';
}

function renderAccessLevel(accessLevel, type) {
    const normalized = trimToNull(accessLevel) || 'READ';
    if (type !== 'display') {
        return normalized;
    }

    const label = normalized.charAt(0) + normalized.slice(1).toLowerCase();
    return '<span class="badge role-access-badge role-access-badge--' + normalized.toLowerCase() + '">' + escapeHtml(label) + '</span>';
}

function renderPlainText(value, type) {
    const normalized = trimToNull(value) || '-';
    if (type !== 'display') {
        return normalized;
    }
    return escapeHtml(normalized);
}

function normalizeWholeNumber(value, fallbackValue) {
    const normalized = Number(value);
    if (!Number.isFinite(normalized) || normalized < 0) {
        return fallbackValue;
    }
    return Math.round(normalized);
}

function trimToNull(value) {
    if (value == null) {
        return null;
    }

    const normalized = String(value).trim();
    return normalized.length === 0 ? null : normalized;
}

function escapeHtml(value) {
    return $('<div/>').text(value == null ? '' : String(value)).html();
}


