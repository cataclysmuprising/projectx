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
        "render": function (data, type, full, meta) {
            if (full.status) {
                if (full.status === "ACTIVE") {
                    return '<i class="fa fa-check text-green"></i>';
                }
                else if (full.status === "SUSPENDED") {
                    return '<i class="fa fa-exclamation-triangle text-yellow"></i>';
                }
                else {
                    return '-';
                }
            }
            else {
                return '-';
            }

        },
        "sClass": "text-center",
    }, {
        "mData": "createdDate",
        "sClass": "text-center"
    },];
    if (hasAnyAuthority("administratorDetail", "administratorEdit", "administratorRemove")) {
        columns.push({
            "render": function (data, type, full, meta) {
                let detailButton = {
                    label: "View",
                    authorityName: "administratorDetail",
                    url: getContextPath() + "/web/sec/administrator/" + full.id,
                    styleClass: "",
                    data_id: full.id
                };
                let editButton = {
                    label: "Edit",
                    authorityName: "administratorEdit",
                    url: getContextPath() + "/web/sec/administrator/" + full.id + '/edit',
                    styleClass: "",
                    data_id: full.id
                };
                let removeButton = {
                    label: "Remove",
                    authorityName: "administratorRemove",
                    url: getContextPath() + "/web/sec/administrator/" + full.id + '/delete',
                    styleClass: "remove",
                    data_id: full.id
                };
                return generateAuthorizedButtonGroup([detailButton, editButton, removeButton]);
            },
            "bSortable": false,
            "sClass": "text-center"
        });
    }
    dataTable = $('#tblAdministrator').DataTable({
        aoColumns: columns,
        "aaSorting": [],
        columnDefs: [{
            width: 150,
            targets: 1
        }, {
            width: 150,
            targets: 2
        },],
        ajax: {
            type: "POST",
            url: getApiResourcePath() + 'sec/administrator/search/paging',
            data: function (d) {
                let criteria = {};

                if (d.order && d.order.length > 0) {
                    const order = d.order[0];
                    const index = order.column;
                    const dir = order.dir;

                    const sortColumn = $("#tblAdministrator thead th").eq(index);
                    const sortKey = sortColumn.data("sort-key");

                    if (sortKey) {
                        criteria.sortKeys = [sortKey];
                        criteria.sortDirs = [dir.toUpperCase()];
                    }
                }

                criteria.offset = d.start;
                criteria.limit = d.length;

                if (isNotEmpty($("#lstStatus").val())) {
                    criteria.status = $("#lstStatus").val();
                }

                const word = $("#keyword").val();
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