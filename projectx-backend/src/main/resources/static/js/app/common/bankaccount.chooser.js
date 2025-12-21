// https://stackoverflow.com/a/24359224/1531064
// https://jsfiddle.net/nUzJN/286/
(function ($) {

    $.BankAccountChooser = function (element, options) {
        let $elementCount = "-" + $('.modal-bankaccount-chooser').length;
        // Declare UI Template
        const chooseBankAccountModelTemplate = ({title}) => `
            <div class="modal fade modal-bankaccount-chooser" id="modal-bankaccount-chooser${$elementCount}">
                <div class="modal-dialog modal-xl modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h4 class="modal-title align-self-center">${title}</h4>
                            <button aria-hidden="true" class="close modal-close" type="button">&times;</button>
                        </div>
                        <div class="modal-body">
                            <div class="row filter-container mb-3 text-center">
                                <div class="col-12 col-lg-6 mt-3">
                                    <div class="form-group">
                                        <div class="input-group">
                                            <input class="form-control form-control-sm" id="txt-search-bankaccount-accno${$elementCount}" placeholder="Enter Account Number" type="text" value=""/>
                                            <div class="input-group-append">
                                                <button class="btn btn-primary" id="btn-search-bankaccount-accno${$elementCount}" type="button">
                                                    <i class="fas fa-search"></i>
                                                    <span class="search-button-text">Search</span>
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-12">
                                <p class="text-center text-orange mt-1 d-none" id="bankaccount-chooser-message-box" style="background-color: #f9ebda;font-size: .75rem;border: 1px dashed;padding: .75rem;">
                                </p>
                            </div>
                            <div class="row">
                                <div class="col-12">
                                    <table class="table table-bordered tbl-bankaccount-result" id="tbl-bankaccount-result${$elementCount}">
                                        <thead>
                                        <tr>
                                            <th>Account No. </th>
                                            <th>Name</th>
                                            <th>Account Type</th>
                                            <th>Bank</th>
                                            <th>Branch</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        </tbody>
                                    </table>
                                </div>  
                            </div>                                                            
                        </div>
                        <div class="modal-footer text-center">
                            <button class="btn btn-flat btn-success" id="btn-bankaccount-chooser-createnew${$elementCount}" type="button"><i class="fas fa-plus-circle"></i> Create New</button>
                            <button class="btn btn-flat btn-secondary" id="btn-bankaccount-chooser-choose${$elementCount}" type="button"><i class="fas fa-check-square"></i> Select</button>
                            <button class="btn btn-flat btn-danger modal-close" type="button"><i class="fas fa-times"></i> Close</button>
                        </div>
                    </div>
                </div>
            </div>`;

        // plugin's default options
        // this is private property and is accessible only from inside the plugin
        let events = {

            // if your plugin is event-driven, you may provide callback capabilities
            // for its events. execute these functions before or after events of your
            // plugin, so that users may customize those particular events without
            // changing the plugin's code
            onChooseBankAccount: function (obj) {
                if (options.onChooseBankAccount !== undefined) {
                    options.onChooseBankAccount(plugin, $element, obj);
                }
            },

            onModalShow: function (obj) {
                initBankAccountDataTable();
                $modalBankAccountChooser.modal({
                    backdrop: 'static',
                    keyboard: false
                });
            },

            onModalClose: function (options) {
                $modalBankAccountChooser.modal("hide");
                // reset form datas
                $txtSearchBankAccountByAccNo.val("");
                // hide message box
                $('#bankaccount-chooser-message-box').removeClass('d-block').addClass('d-none');
                // clear bankaccount result tables
                $tblBankAccountDataTable.clear().destroy();
            }

        }

        // to avoid confusions, use "plugin" to reference the
        // current instance of the object
        let plugin = this;

        // this will hold the merged default, and user-provided options
        // plugin's properties will be available through this object like:
        // plugin.settings.propertyName from inside the plugin or
        // element.data('BankAccountChooser').settings.propertyName from outside the plugin,
        // where "element" is the element the plugin is attached to;
        plugin.settings = {};
        plugin.searchCriteria = {};

        let $element = $(element); // reference to the jQuery version of DOM element

        // Declare functional Elements to variables
        let $modalBankAccountChooser;
        let $txtSearchBankAccountByAccNo;
        let $btnSearchBankAccountByAccNo;
        let $btnCreateNewBankAccount;
        let $tblBankAccountDataTable;
        let $btnChooseBankAccount;
        let $bankAccountTableSelector;

        // the "constructor" method that gets called when the object is created
        plugin.init = function () {
            // the plugin's final properties are the merged default and
            // user-provided options (if any)
            plugin.settings = $.extend({}, events, options);
            if (!plugin.settings.masterdatas) {
                plugin.settings.masterdatas = {};
            }
            $('body').append([{title: plugin.settings.chooseModalTitle}].map(chooseBankAccountModelTemplate).join(''));

            // add the custom css rules for plugin
            CSS(`
                .background-modal {
                    opacity: 0;
                    z-index: -10 !important;
                }
                
                .tbl-bankaccount-result tr:hover {
                    background-color: #f2f2f2;
                    cursor: pointer;
                }
                
                .tbl-bankaccount-result tr.bankaccount-selected td:not(.dataTables_empty) {
                    background-color: #6c757d !important;
                }
                
                .tbl-bankaccount-result tr.bankaccount-selected td:not(.dataTables_empty), .tbl-bankaccount-result tr.bankaccount-selected td a {
                    color: #fff !important;
                }
            `);

            // bind variables to DOM elements
            $modalBankAccountChooser = $('#modal-bankaccount-chooser' + $elementCount);
            $txtSearchBankAccountByAccNo = $('#txt-search-bankaccount-accno' + $elementCount);
            $btnSearchBankAccountByAccNo = $('#btn-search-bankaccount-accno' + $elementCount);
            $btnChooseBankAccount = $('#btn-bankaccount-chooser-choose' + $elementCount);
            $bankAccountTableSelector = '#tbl-bankaccount-result' + $elementCount;

            // modal close event
            $modalBankAccountChooser.find('.modal-close').on('click', function (e) {
                events.onModalClose(options);
            });

            // bind event for 'select' button event
            $btnChooseBankAccount.on('click', function (e) {
                let selectedBankAccount = plugin.getSelectedBankAccount();
                if (selectedBankAccount) {
                    events.onChooseBankAccount(selectedBankAccount);
                    events.onModalClose(options);
                }
                else {
                    let message = "Search entry datas by <kbd>'Bankaccount Number'</kbd> information first and select the related entry by <strong>selecting the row</strong> of results. If there has no-related informations, please <strong>check your input datas</strong> or <strong>click</strong> the <code>Create New</code> button to add informations.";
                    let messageBox = $('#bankaccount-chooser-message-box');
                    messageBox.html(message);
                    messageBox.removeClass('d-none').addClass('d-block');

                }
            });

            // bind event for 'Create New' button event
            if (plugin.settings.enableCreateNew === true) {
                $btnCreateNewBankAccount = $('#btn-bankaccount-chooser-createnew' + $elementCount)
                if (plugin.settings.ownerId > 0) {
                    $btnCreateNewBankAccount.BankAccountCreator({
                        createModalTitle: plugin.settings.createModalTitle,
                        masterdatas: plugin.settings.masterdatas,
                        ownerId: plugin.settings.ownerId,
                        onModalShow: function (plugin, $element, obj) {
                            // close current chooser modal
                            //events.onModalClose(options);
                            $modalBankAccountChooser.addClass("background-modal");
                        },
                        onModalClose: function (plugin, $element, obj) {
                            $modalBankAccountChooser.removeClass("background-modal");
                        },
                        onCreateBankaccount: function (plugin, $element, obj) {
                            let dataArr = [];
                            dataArr.push(obj);
                            $tblBankAccountDataTable.clear().destroy();
                            initBankAccountDataTable(dataArr);
                        },
                    });
                }
                else {
                    $btnCreateNewBankAccount.on('click', function (e) {
                        notify("Bank Account", `Please select ${plugin.settings.owner} first.`, 'error')
                    })
                }
            }
            else {
                $('#btn-bankaccount-chooser-createnew' + $elementCount).remove();
            }

            // add click event to plugin's element
            $element.on("click", function () {
                events.onModalShow(options);
            });
        }

        // private methods
        // these methods can be called only from inside the plugin like:
        // methodName(arg1, arg2, ... argn)

        let initBankAccountDataTable = function (data) {

            let columns = [
                {
                    "render": function (data, type, full, meta) {
                        return full.accountNo;
                    },
                    "bSortable": false,
                },
                {
                    "render": function (data, type, full, meta) {
                        return full.accountName;
                    },
                    "bSortable": false,
                },
                {
                    "render": function (data, type, full, meta) {
                        if (isNotEmpty(full.bankAccountType)) {
                            return full.bankAccountType.name;
                        }
                        else {
                            return '-';
                        }
                    },
                    "bSortable": false,
                },
                {
                    "render": function (data, type, full, meta) {
                        return full.bank.name;
                    },
                    "bSortable": false,
                },
                {
                    "render": function (data, type, full, meta) {
                        return full.branch.name;
                    },
                    "bSortable": false,
                },
            ];

            $tblBankAccountDataTable = $($bankAccountTableSelector).DataTable({
                data: data,
                aoColumns: columns,
                "aaSorting": [],
                columnDefs: [{}],
                deferLoading: data ? data.length : 0,
                ajax: {
                    type: "POST",
                    url: getApiResourcePath() + "sec/bankAccount/search/paging",
                    data: function (d) {
                        if (d.order.length > 0) {
                            let index = $(d.order[0])[0].column;
                            let dir = $(d.order[0])[0].dir;
                            let head = $($bankAccountTableSelector).find("thead");
                            let sortColumn = head.find("th:eq(" + index + ")");
                            plugin.searchCriteria.sortType = dir.toUpperCase();
                            plugin.searchCriteria.sortProperty = $(sortColumn).attr("data-sort-key");
                        }
                        plugin.searchCriteria.offset = d.start;
                        plugin.searchCriteria.limit = d.length;

                        return JSON.stringify(plugin.searchCriteria);
                    },
                },
                initComplete: function () {
                    let api = this.api();
                    $btnSearchBankAccountByAccNo.off('click').on('click', function (e) {
                        if ($txtSearchBankAccountByAccNo.val().trim().length >= 5) {
                            plugin.searchCriteria.accountNo = $txtSearchBankAccountByAccNo.val().trim();
                            api.search(this.value).draw();
                        }
                        else {
                            alert("Invalid/Incomplete Bank Account Number");
                        }
                    });
                },
                drawCallback: function (settings) {
                    $($bankAccountTableSelector + ' tbody tr').on('click', function (e) {
                        if ($tblBankAccountDataTable.data().count() > 0) {
                            $($bankAccountTableSelector + ' tbody tr').removeClass('bankaccount-selected');
                            $(this).addClass('bankaccount-selected');
                        }
                    });
                }
            });
        }

        // public methods
        // these methods can be called like:
        // plugin.methodName(arg1, arg2, ... argn) from inside the plugin or
        // element.data('pluginName').publicMethod(arg1, arg2, ... argn) from outside, $(this).data('pluginName').publicMethod()
        // the plugin, where "element" is the element the plugin is attached to;

        plugin.getSelectedBankAccount = function () {
            return $tblBankAccountDataTable.rows('.bankaccount-selected').data()[0];
        }

        plugin.setOwnerId = function (ownerId) {
            plugin.settings.ownerId = ownerId;
            if (plugin.settings.enableCreateNew === true && ownerId > 0) {
                $btnCreateNewBankAccount.off('click')
                $btnCreateNewBankAccount.BankAccountCreator({
                    createModalTitle: plugin.settings.createModalTitle,
                    masterdatas: plugin.settings.masterdatas,
                    ownerId: plugin.settings.ownerId,
                    onModalShow: function (plugin, $element, obj) {
                        // close current chooser modal
                        //events.onModalClose(options);
                        $modalBankAccountChooser.addClass("background-modal");
                    },
                    onModalClose: function (plugin, $element, obj) {
                        $modalBankAccountChooser.removeClass("background-modal");
                    },
                    onCreateBankaccount: function (plugin, $element, obj) {
                        let dataArr = [];
                        dataArr.push(obj);
                        $tblBankAccountDataTable.clear().destroy();
                        initBankAccountDataTable(dataArr);
                    },
                });
            }
        }

        // fire up the plugin!
        // call the "constructor" method
        plugin.init();

    }

    // add the plugin to the jQuery.fn object
    $.fn.BankAccountChooser = function (options) {

        // iterate through the DOM elements we are attaching the plugin to
        return this.each(function () {

            // if plugin has not already been attached to the element
            if (undefined === $(this).data('BankAccountChooser')) {

                // create a new instance of the plugin
                // pass the DOM element and the user-provided options as arguments
                let plugin = new $.BankAccountChooser(this, options);

                // in the jQuery version of the element
                // store a reference to the plugin object
                // you can later access the plugin and its methods and properties like
                // element.data('BankAccountChooser').publicMethod(arg1, arg2, ... argn) or
                // element.data('BankAccountChooser').settings.propertyName
                $(this).data('BankAccountChooser', plugin);
            }
        });
    }

})(jQuery);
