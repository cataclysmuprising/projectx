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
    if (hasAnyAuthority("roleEdit", "roleRemove")) {
        columns.push({
            "render": function (data, type, full, meta) {
                let editButton = {
                    label: "Edit",
                    authorityName: "roleEdit",
                    showElementOnExpression: full.roleType === 'custom',
                    url: getContextPath() + "/web/sec/role/" + full.id + '/edit',
                    styleClass: "",
                    data_id: full.id
                };
                let removeButton = {
                    label: "Remove",
                    authorityName: "roleRemove",
                    showElementOnExpression: full.roleType === 'custom',
                    url: getContextPath() + "/web/sec/role/" + full.id + '/delete',
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
            url: getApiResourcePath() + 'sec/role/search/paging',
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