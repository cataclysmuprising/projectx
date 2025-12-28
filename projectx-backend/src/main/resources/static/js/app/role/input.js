let tblAdministrator, tblAction;

function init() {
    initAdministratorTable();
    initActionTable();
    initValidator();
    loadPageNames();
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
                let criteria = {};
                if (d.order && d.order.length > 0) {
                    const order = d.order[0];
                    const index = order.column;
                    const dir = order.dir;

                    const sortColumn = $("#tblRole thead th").eq(index);
                    const sortKey = sortColumn.data("sort-key");

                    if (sortKey) {
                        criteria.sortKeys = [sortKey];
                        criteria.sortDirs = [dir.toUpperCase()];
                    }
                }
                criteria.offset = d.start;
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
        "mData": "page",
        "sClass": "text-left"
    }, {
        "mData": "displayName",
        "bSortable": false,
        "sClass": "text-left"
    }, {
        "mData": "description",
        "bSortable": false,
        "sClass": "nowrap_text",
    }];

    tblAction = $('#tblAction').DataTable({
        aoColumns: columns,
        "aaSorting": [],
        "pageLength": SECONDARY_ROW_PER_PAGE,
        ajax: {
            type: "POST",
            url: getApiResourcePath() + 'sec/action/search/paging',
            data: function (d) {
                let criteria = {};
                if (d.order && d.order.length > 0) {
                    const order = d.order[0];
                    const index = order.column;
                    const dir = order.dir;

                    const sortColumn = $("#tblRole thead th").eq(index);
                    const sortKey = sortColumn.data("sort-key");

                    if (sortKey) {
                        criteria.sortKeys = [sortKey];
                        criteria.sortDirs = [dir.toUpperCase()];
                    }
                }
                criteria.offset = d.start;
                criteria.limit = d.length;
                if (isNotEmpty($("#pageName").val())) {
                    criteria.page = $("#pageName").val();
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

function loadPageNames() {
    let criteria = {};
    $.ajax({
        type: "GET",
        url: getApiResourcePath() + 'sec/action/pages',
        data: criteria,
        success: function (data) {
            let options = [];
            options.push("<option data-srcode='' value=''>Page (All)</option>");
            $.each(data, function (key, item) {
                let option = "<option  value='" + item + "'>" + item + "</option>";
                options.push(option);
            });
            $("#pageName").html(options).selectpicker('refresh');
        }
    });
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


