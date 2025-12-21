let dataTable;

function init() {
    initDataTable();
}

function bind() {

    $("#btnSearch").on('click', function (e) {
        dataTable.search($(this).val()).draw();
    });

    $("#btnReset").on('click', function (e) {
        $('form').trigger('reset');
        dataTable.search($(this).val()).draw();
    });

}

function initDataTable() {
    let columns = [{
        "mData": "name",
        "sClass": "text-left"
    }, {
        "mData": "description",
        "bSortable": false,
        "sClass": "text-left"
    },];
    if (hasAuthority("roleEdit") || hasAuthority("roleRemove")) {
        columns.push({
            "render": function (data, type, full, meta) {
                let editButton = {
                    label: "Edit",
                    authorityName: "roleEdit",
                    showElementOnExpression: full.roleType === 'custom',
                    url: getContextPath() + "/sec/role/" + full.id + '/edit',
                    styleClass: "",
                    data_id: full.id
                };
                let removeButton = {
                    label: "Remove",
                    authorityName: "roleRemove",
                    showElementOnExpression: full.roleType === 'custom',
                    url: getContextPath() + "/sec/role/" + full.id + '/delete',
                    styleClass: "remove",
                    data_id: full.id
                };
                return generateAuthorizedButtonGroup([editButton, removeButton]);
            },
            "bSortable": false,
            "sClass": "text-center"
        });
    }
    dataTable = $('#tblRole').DataTable({
        aoColumns: columns,
        "aaSorting": [],
        columnDefs: [{}],
        ajax: {
            type: "POST",
            url: getApiResourcePath() + 'sec/roles/search/paging',
            data: function (d) {
                let criteria = {};
                if (d.order.length > 0) {
                    let index = $(d.order[0])[0].column;
                    let dir = $(d.order[0])[0].dir;
                    let head = $("#tblRole").find("thead");
                    let sortColumn = head.find("th:eq(" + index + ")");
                    criteria.sortType = dir.toUpperCase();
                    criteria.sortProperty = $(sortColumn).attr("data-sort-key");
                }
                criteria.offset = d.start;
                criteria.limit = d.length;

                let word = $("#keyword").val();
                if (isNotEmpty(word)) {
                    criteria.keyword = word.trim();
                }
                return JSON.stringify(criteria);
            }
        },
        initComplete: function () {
            let api = this.api();
            $('#keyword').off('.DT').on('keyup.DT', function (e) {
                if (e.keyCode === 13) {
                    api.search(this.value).draw();
                }
            });
        },
        drawCallback: function (settings) {
            bindRemoveButtonEvent();
        }
    });
}