// https://stackoverflow.com/a/24359224/1531064
// https://jsfiddle.net/nUzJN/286/
(function ($) {

    $.EntityChooser = function (element, options) {
        let $elementCount = "-" + $('.modal-entity-chooser').length;
        // Declare UI Template
        const chooseEntityModelTemplate = ({title}) => `
                <div class="modal fade modal-entity-chooser" id="modal-entity-chooser${$elementCount}">
                    <div class="modal-dialog modal-xl modal-dialog-centered" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h4 class="modal-title align-self-center">${title}</h4>
                            <ul class="nav nav-pills ml-3" id="entity-chooser-type-menu${$elementCount}">
                                    <li class="nav-item">
                                        <a class="nav-link active" data-toggle="tab" href="#tab-entity-chooser-individual${$elementCount}">Individual</a>
                                    </li>
                                    <li class="nav-item">
                                        <a class="nav-link" data-toggle="tab" href="#tab-entity-chooser-company-org${$elementCount}">Company/Organization</a>
                                    </li>
                                </ul>
                                <button aria-hidden="true" class="close modal-close" type="button">&times;</button>
                            </div>
                            <div class="modal-body">
                                <div class="tab-content">
                                    <div class="tab-pane active" id="tab-entity-chooser-individual${$elementCount}">
                                        <div class="row filter-container mb-3">
                                            <div class="col-12 col-lg-7 nrc-container">
                                                <label>NRC</label>
                                                <div class="row">
                                                    <div class="col-6 col-lg-3">
                                                        <div class="form-group">
                                                            <div class="input-group">
                                                                <select class="dropdown-select nrc-state-selectpicker form-control show-tick" data-size="5" title="Region"></select>
                                                                <div class="input-group-append">
                                                                    <span class="input-group-text pl-3 pr-3">/</span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div class="col-6 col-lg-3">
                                                        <div class="form-group">
                                                            <select class="dropdown-select nrc-township-selectpicker form-control show-tick" disabled data-live-search="true" data-size="5" title="Township"></select>
                                                        </div>
                                                    </div>
                                                    <div class="col-6 col-lg-2">
                                                        <div class="form-group">
                                                            <select class="dropdown-select nrc-type-selectpicker form-control show-tick" data-size="6" title="Type">
                                                                <option value="နိုင်" selected>နိုင်</option>
                                                                <option value="ဧည့်">ဧည့်</option>
                                                                <option value="ပြု">ပြု</option>
                                                                <option value="သာသနာ">သာသနာ</option>
                                                                <option value="ယာယီ">ယာယီ</option>
                                                                <option value="စ">စ</option>
                                                            </select>
                                                        </div>
                                                    </div>
                                                    <div class="col-6 col-lg-4">
                                                        <div class="form-group">
                                                            <div class="input-group">
                                                                <input class="form-control form-control-sm number-only-input" id="txt-search-entity-nrc${$elementCount}" placeholder="NRC Number" maxlength="6" type="text" value=""/>
                                                                <div class="input-group-append">
                                                                   <button class="btn btn-primary" id="btn-search-entity-nrc${$elementCount}" type="button">
                                                                       <i class="fas fa-search"></i>
                                                                       <span class="search-button-text">Search</span>
                                                                   </button>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="col-12 col-lg-5">
                                                <div class="form-group">
                                                    <label>Passport No</label>
                                                    <div class="input-group">
                                                        <input class="form-control form-control-sm" id="txt-search-entity-passport${$elementCount}" placeholder="Enter passport number" type="text" value=""/>
                                                        <div class="input-group-append">
                                                            <button class="btn btn-primary" id="btn-search-entity-passport${$elementCount}" type="button">
                                                                <i class="fas fa-search"></i>
                                                                <span class="search-button-text">Search</span>
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col-12">
                                            <p class="text-center text-orange mt-1 entity-chooser-message-box d-none" style="background-color: #f9ebda;font-size: .75rem;border: 1px dashed;padding: .75rem;">
                                            </p>
                                        </div>
                                        <table class="table table-bordered tbl-entity-result" id="tbl-entity-individual-result${$elementCount}">
                                            <thead>
                                            <tr>
                                                <th>Name</th>
                                                <th>NRC/Passport</th>
                                                <th>Father Name</th>
                                                <th>Phone No.</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            </tbody>
                                        </table>
                                    </div>
                                    <div class="tab-pane" id="tab-entity-chooser-company-org${$elementCount}">
                                        <div class="row filter-container mb-3 text-center">
                                            <div class="col-12 col-lg-6 mt-3">
                                                <div class="form-group">
                                                    <div class="input-group">
                                                        <input class="form-control form-control-sm" id="txt-search-entity-regno${$elementCount}" placeholder="Enter Registeration Number" type="text" value=""/>
                                                        <div class="input-group-append">
                                                            <button class="btn btn-primary" id="btn-search-entity-regno${$elementCount}" type="button">
                                                                <i class="fas fa-search"></i>
                                                                <span class="search-button-text">Search</span>
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col-12">
                                            <p class="text-center text-orange mt-1 entity-chooser-message-box d-none" style="background-color: #f9ebda;font-size: .75rem;border: 1px dashed;padding: .75rem;">
                                            </p>
                                        </div>
                                        <div class="row">
                                            <div class="col-12">
                                                <table class="table table-bordered tbl-entity-result" id="tbl-entity-company-org-result${$elementCount}">
                                                    <thead>
                                                    <tr>
                                                        <th>Registeration No. </th>
                                                        <th>Name</th>
                                                        <th>Business Type</th>
                                                        <th>Contact Phone</th>
                                                    </tr>
                                                    </thead>
                                                    <tbody>
                                                    </tbody>
                                                </table>
                                            </div>  
                                        </div>                                         
                                    </div>   
                                </div>                     
                            </div>
                            <div class="modal-footer text-center">
                                <button class="btn btn-flat btn-success" id="btn-entity-chooser-createnew${$elementCount}" type="button"><i class="fas fa-plus-circle"></i> Create New</button>
                                <button class="btn btn-flat btn-secondary" id="btn-entity-chooser-choose${$elementCount}" type="button"><i class="fas fa-check-square"></i> Select</button>
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
            onChooseEntity: function (obj) {
                if (options.onChooseEntity !== undefined) {
                    options.onChooseEntity(plugin, $element, obj);
                }
            },

            onModalShow: function (obj) {
                if (plugin.settings.isIndividualEntityOnly === true) {
                    $entityTypeMenu.hide();
                }
                initIndividualEntityDataTable();
                initCompanyOrgEntityDataTable();
                $modalEntityChooser.modal({
                    backdrop: 'static',
                    keyboard: false
                });
            },

            onModalClose: function (options) {
                $modalEntityChooser.modal("hide");
                // reset form datas
                $txtNrcNo.val("");
                $txtPassportNo.val("");
                $lstNrcRegionCode.selectpicker('val', '').selectpicker('refresh');
                $lstTownshipCode.prop('disabled', true).selectpicker('val', '').selectpicker('refresh');
                $lstNrcType.selectpicker('val', 'နိုင်').selectpicker('refresh');
                $txtRegNo.val("");
                // hide message box
                $('.entity-chooser-message-box').removeClass('d-block').addClass('d-none');
                // clear entity result tables
                $tblIndividualEntityDataTable.clear().destroy();
                $tblCompanyOrgEntityDataTable.clear().destroy();
                // re-select the 'Individual' Tab
                $('.nav-link[href="#tab-entity-chooser-individual' + $elementCount + '"]').trigger('click');
            }

        }

        // to avoid confusions, use "plugin" to reference the
        // current instance of the object
        let plugin = this;

        // this will hold the merged default, and user-provided options
        // plugin's properties will be available through this object like:
        // plugin.settings.propertyName from inside the plugin or
        // element.data('EntityChooser').settings.propertyName from outside the plugin,
        // where "element" is the element the plugin is attached to;
        plugin.settings = {};
        plugin.entitySearchCriteria = {};

        let $element = $(element); // reference to the jQuery version of DOM element

        // Declare functional Elements to variables
        let $modalEntityChooser;
        let $entityTypeMenu;
        let $nrcContainerElem;
        let $lstNrcRegionCode, $lstTownshipCode, $lstNrcType, $txtNrcNo, $txtPassportNo, $txtRegNo;
        let $btnSearchEntityByNRC, $btnSearchEntityByPassport, $btnSearchEntityByRegNo;
        let $btnChooseEntity, $btnCreateNewEntity;

        let $tblIndividualEntityDataTable, $individualEntityTableSelector, $tblCompanyOrgEntityDataTable, $companyOrgEntityTableSelector;

        // the "constructor" method that gets called when the object is created
        plugin.init = function () {
            // the plugin's final properties are the merged default and
            // user-provided options (if any)
            plugin.settings = $.extend({}, events, options);
            if (!plugin.settings.masterdatas) {
                plugin.settings.masterdatas = {};
            }
            $('body').append([{title: plugin.settings.chooseModalTitle}].map(chooseEntityModelTemplate).join(''));

            // add the custom css rules for plugin
            CSS(`
                .background-modal {
                    opacity: 0;
                    z-index: -10 !important;
                }
                
                .tbl-entity-result tr:hover {
                    background-color: #f2f2f2;
                    cursor: pointer;
                }
                
                .tbl-entity-result tr.entity-selected td:not(.dataTables_empty) {
                    background-color: #6c757d !important;
                }
                
                .tbl-entity-result tr.entity-selected td:not(.dataTables_empty), .tbl-entity-result tr.entity-selected td a {
                    color: #fff !important;
                }
            `);

            // bind variables to DOM elements
            $modalEntityChooser = $('#modal-entity-chooser' + $elementCount);
            $entityTypeMenu = $('#entity-chooser-type-menu' + $elementCount);
            $nrcContainerElem = $('#tab-entity-chooser-individual' + $elementCount).find('.nrc-container');
            $lstNrcRegionCode = $nrcContainerElem.find('.nrc-state-selectpicker');
            $lstTownshipCode = $nrcContainerElem.find('.nrc-township-selectpicker');
            $lstNrcType = $nrcContainerElem.find('.nrc-type-selectpicker');
            $txtNrcNo = $('#txt-search-entity-nrc' + $elementCount);
            $txtPassportNo = $('#txt-search-entity-passport' + $elementCount);
            $txtRegNo = $('#txt-search-entity-regno' + $elementCount);
            $btnSearchEntityByNRC = $('#btn-search-entity-nrc' + $elementCount);
            $btnSearchEntityByPassport = $('#btn-search-entity-passport' + $elementCount);
            $btnSearchEntityByRegNo = $('#btn-search-entity-regno' + $elementCount);
            $btnChooseEntity = $('#btn-entity-chooser-choose' + $elementCount);
            $individualEntityTableSelector = '#tbl-entity-individual-result' + $elementCount;
            $companyOrgEntityTableSelector = '#tbl-entity-company-org-result' + $elementCount;

            // modal close event
            $modalEntityChooser.find('.modal-close').on('click', function (e) {
                events.onModalClose(options);
            });

            if ($.fn.inputmask) {
                $(".number-only-input").inputmask({
                    regex: "\\d+",
                    'placeholder': ''
                });
            }
            // bind event for 'select' button event
            $btnChooseEntity.on('click', function (e) {
                let selectedEntity = plugin.getSelectedEntity();
                if (selectedEntity) {
                    events.onChooseEntity(selectedEntity);
                    events.onModalClose(options);
                }
                else {
                    let messageBox;
                    let message;
                    if ($('#tab-entity-chooser-individual' + $elementCount).hasClass('active')) {
                        message = "Search entry datas by <code>'NRC/Passport'</code> information first and select the related entry by <strong>selecting the row</strong> of results. If there has no-related informations, please <strong>check your input datas</strong> or <strong>click</strong> the <kbd>Create New</kbd> button to add informations.";
                        messageBox = $('#tab-entity-chooser-individual' + $elementCount + ' .entity-chooser-message-box');
                    }
                    else {
                        message = "Search entry datas by <code>'Registeration Number'</code> information first and select the related entry by <strong>selecting the row</strong> of results. If there has no-related informations, please <strong>check your input datas</strong> or <strong>click</strong> the <kbd>Create New</kbd> button to add informations.";
                        messageBox = $('#tab-entity-chooser-company-org' + $elementCount + ' .entity-chooser-message-box');
                    }
                    messageBox.html(message);
                    messageBox.removeClass('d-none').addClass('d-block');

                }
            });

            // bind event for 'Create New' button event
            if (plugin.settings.enableCreateNew === true) {
                $btnCreateNewEntity = $('#btn-entity-chooser-createnew' + $elementCount).EntityCreator({
                    createModalTitle: plugin.settings.createModalTitle,
                    isIndividualEntityOnly: plugin.settings.isIndividualEntityOnly ? plugin.settings.isIndividualEntityOnly : false,
                    masterdatas: plugin.settings.masterdatas,
                    onModalShow: function (plugin, $element, obj) {
                        // close current chooser modal
                        //events.onModalClose(options);
                        $modalEntityChooser.addClass("background-modal");
                    },
                    onCreateEntity: function (obj) {
                        let dataArr = [];
                        dataArr.push(obj);
                        if (obj.customerType === 'INDIVIDUAL') {
                            $tblIndividualEntityDataTable.clear().destroy();
                            initIndividualEntityDataTable(dataArr);
                            $('.nav-link[href="#tab-entity-chooser-individual' + $elementCount + '"]').trigger('click');
                        }
                        else {
                            $tblCompanyOrgEntityDataTable.clear().destroy();
                            initCompanyOrgEntityDataTable(dataArr);
                            $('.nav-link[href="#tab-entity-chooser-company-org' + $elementCount + '"]').trigger('click');
                        }
                    },
                    onModalClose: function (plugin, $element, obj) {
                        $modalEntityChooser.removeClass("background-modal");
                    }
                });
            }
            else {
                $('#btn-entity-chooser-createnew' + $elementCount).remove();
            }

            // add click event to plugin's element
            $element.on("click", function () {
                events.onModalShow(options);
            });

            // load related master datas
            initNRCStates(false);
            $lstNrcRegionCode.on("change", function (e) {
                loadNRCTownships($(this).find("option:selected").data("state-code"), $(this).closest('.nrc-container'));
            });
        }

        // private methods
        // these methods can be called only from inside the plugin like:
        // methodName(arg1, arg2, ... argn)

        let initNRCStates = function (isByForce) {
            if (isByForce === false && plugin.settings.masterdatas.nrcStates) {
                let options = [];
                $.each(plugin.settings.masterdatas.nrcStates, function (key, item) {
                    let option = "<option data-state-code = '" + item.stateCode + "' value='" + item.numberMM + "'>" + item.numberMM + "</option>";
                    options.push(option);
                });
                $lstNrcRegionCode.html(options).selectpicker('refresh');
            }
            else {
                let criteria = {};
                $.ajax({
                    type: "GET",
                    async: false,
                    url: getApiResourcePath() + 'sec/master-data/general/nrc-states',
                    data: criteria,
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            plugin.settings.masterdatas.nrcStates = response;
                            let options = [];
                            $.each(response, function (key, item) {
                                let option = "<option data-state-code = '" + item.stateCode + "' value='" + item.numberMM + "'>" + item.numberMM + "</option>";
                                options.push(option);
                            });
                            $lstNrcRegionCode.html(options).selectpicker('refresh');
                        }
                        else {
                            notify("Error", "Failed to fetch NRC state informations.", "error");
                        }
                    }
                });
            }
        }

        let loadNRCTownships = function (stateCode, parentContainer) {
            if (isNotEmpty(stateCode)) {
                let criteria = {};
                criteria.stateCode = stateCode;
                $.ajax({
                    type: "GET",
                    url: getApiResourcePath() + 'sec/master-data/general/nrc-townships',
                    data: criteria,
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            let options = [];
                            $.each(response, function (key, item) {
                                let option = "<option value='" + item.shortMM + "'>" + item.shortMM + "</option>";
                                options.push(option);
                            });

                            let disabledAttr = $(parentContainer).find(".nrc-state-selectpicker").attr('disabled');
                            let townshipSelectPicker = $(parentContainer).find(".nrc-township-selectpicker");
                            if (typeof disabledAttr !== 'undefined' && disabledAttr !== false) {
                                townshipSelectPicker.prop('disabled', true);
                            }
                            else {
                                townshipSelectPicker.prop('disabled', false);
                            }
                            townshipSelectPicker.html(options).selectpicker('refresh');
                        }
                        else {
                            notify("Error", "Failed to fetch NRC township informations.", "error");
                        }
                    }
                });
            }
        }

        let initIndividualEntityDataTable = function (data) {

            let columns = [
                {
                    "render": function (data, type, full, meta) {
                        return '<a target="_blank" href="' + getContextPath() + '/sec/entity/' + full.id + '/detail">' + full.name + '</a>';
                    },
                    "bSortable": false,
                },
                {
                    "render": function (data, type, full, meta) {
                        if (isNotEmpty(full.nrcNo)) {
                            return '<span>' + full.nrcRegionCode + "/" + full.nrcTownshipCode + "(" + full.nrcType + ")" + full.nrcNo + '</span>';
                        }
                        else {
                            return isNotEmpty(full.passportNo) ? full.passportNo : '<span>-</span>';
                        }
                    },
                    "bSortable": false,
                },
                {
                    "render": function (data, type, full, meta) {
                        if (isNotEmpty(full.fatherName)) {
                            return full.fatherName;
                        }
                        else {
                            return '-';
                        }
                    },
                    "bSortable": false,
                },
                {
                    "render": function (data, type, full, meta) {
                        if (isNotEmpty(full.phone)) {
                            return full.phone;
                        }
                        else {
                            return '-';
                        }
                    },
                    "bSortable": false,
                }
            ];

            $tblIndividualEntityDataTable = $($individualEntityTableSelector).DataTable({
                data: data,
                aoColumns: columns,
                "aaSorting": [],
                columnDefs: [{}],
                deferLoading: data ? data.length : 0,
                ajax: {
                    type: "POST",
                    url: getApiResourcePath() + "sec/entity/search/paging",
                    data: function (d) {
                        if (d.order.length > 0) {
                            let index = $(d.order[0])[0].column;
                            let dir = $(d.order[0])[0].dir;
                            let head = $($individualEntityTableSelector).find("thead");
                            let sortColumn = head.find("th:eq(" + index + ")");
                            plugin.entitySearchCriteria.sortType = dir.toUpperCase();
                            plugin.entitySearchCriteria.sortProperty = $(sortColumn).attr("data-sort-key");
                        }
                        plugin.entitySearchCriteria.offset = d.start;
                        plugin.entitySearchCriteria.limit = d.length;
                        plugin.entitySearchCriteria.regNo = null;

                        return JSON.stringify(plugin.entitySearchCriteria);
                    },
                },
                initComplete: function () {
                    let api = this.api();
                    $btnSearchEntityByNRC.off('click').on('click', function (e) {
                        if (isNotEmpty($lstNrcRegionCode.val()) && isNotEmpty($lstTownshipCode.val()) && isNotEmpty($lstNrcType.val()) && isNotEmpty($txtNrcNo.val())) {
                            plugin.entitySearchCriteria.customerType = 'INDIVIDUAL';
                            plugin.entitySearchCriteria.nrcRegionCode = $lstNrcRegionCode.val().trim();
                            plugin.entitySearchCriteria.nrcTownshipCode = $lstTownshipCode.val().trim();
                            plugin.entitySearchCriteria.nrcType = $lstNrcType.val().trim();
                            plugin.entitySearchCriteria.nrcNo = $txtNrcNo.val().trim();

                            plugin.entitySearchCriteria.passportNo = null;
                            api.search(this.value).draw();
                        }
                        else {
                            alert("Invalid/Incomplete NRC Number");
                        }
                    });
                    $btnSearchEntityByPassport.off('click').on('click', function (e) {
                        if ($txtPassportNo.val().trim().length >= 5) {
                            plugin.entitySearchCriteria.passportNo = $txtPassportNo.val().trim();
                            plugin.entitySearchCriteria.customerType = 'INDIVIDUAL';
                            plugin.entitySearchCriteria.nrcRegionCode = null;
                            plugin.entitySearchCriteria.nrcTownshipCode = null;
                            plugin.entitySearchCriteria.nrcType = null;
                            plugin.entitySearchCriteria.nrcNo = null;
                            api.search(this.value).draw();
                        }
                        else {
                            alert("Invalid/Incomplete Passport Number");
                        }
                    });
                },
                drawCallback: function (settings) {
                    $($individualEntityTableSelector + ' tbody tr').on('click', function (e) {
                        if ($tblIndividualEntityDataTable.data().count() > 0) {
                            $($individualEntityTableSelector + ' tbody tr').removeClass('entity-selected');
                            $(this).addClass('entity-selected');
                        }
                    });
                }
            });
        }

        let initCompanyOrgEntityDataTable = function (data) {

            let columns = [
                {
                    "render": function (data, type, full, meta) {
                        return '<a target="_blank" href="' + getContextPath() + '/sec/entity/' + full.id + '/detail">' + full.regNo + '</a>';
                    },
                    "bSortable": false,
                },
                {
                    "render": function (data, type, full, meta) {
                        return full.name;
                    },
                    "bSortable": false,
                },
                {
                    "render": function (data, type, full, meta) {
                        if (isNotEmpty(full.businessType)) {
                            return full.businessType.name;
                        }
                        else {
                            return '-';
                        }
                    },
                    "bSortable": false,
                },
                {
                    "render": function (data, type, full, meta) {
                        if (isNotEmpty(full.phone)) {
                            return full.phone;
                        }
                        else {
                            return '-';
                        }
                    },
                    "bSortable": false,
                }
            ];

            $tblCompanyOrgEntityDataTable = $($companyOrgEntityTableSelector).DataTable({
                data: data,
                aoColumns: columns,
                "aaSorting": [],
                columnDefs: [{}],
                deferLoading: data ? data.length : 0,
                ajax: {
                    type: "POST",
                    url: getApiResourcePath() + "sec/entity/search/paging",
                    data: function (d) {
                        if (d.order.length > 0) {
                            let index = $(d.order[0])[0].column;
                            let dir = $(d.order[0])[0].dir;
                            let head = $($companyOrgEntityTableSelector).find("thead");
                            let sortColumn = head.find("th:eq(" + index + ")");
                            plugin.entitySearchCriteria.sortType = dir.toUpperCase();
                            plugin.entitySearchCriteria.sortProperty = $(sortColumn).attr("data-sort-key");
                        }
                        plugin.entitySearchCriteria.offset = d.start;
                        plugin.entitySearchCriteria.limit = d.length;

                        plugin.entitySearchCriteria.customerType = 'COMPANY';
                        plugin.entitySearchCriteria.nrcRegionCode = null;
                        plugin.entitySearchCriteria.nrcTownshipCode = null;
                        plugin.entitySearchCriteria.nrcType = null;
                        plugin.entitySearchCriteria.nrcNo = null;
                        plugin.entitySearchCriteria.passportNo = null;

                        return JSON.stringify(plugin.entitySearchCriteria);
                    },
                },
                initComplete: function () {
                    let api = this.api();
                    $btnSearchEntityByRegNo.on('click', function (e) {
                        if ($txtRegNo.val().trim().length >= 5) {
                            plugin.entitySearchCriteria.regNo = $txtRegNo.val().trim();
                            api.search(this.value).draw();
                        }
                        else {
                            alert("Invalid/Incomplete Registeration Number");
                        }
                    });
                },
                drawCallback: function (settings) {
                    $($companyOrgEntityTableSelector + ' tbody tr').on('click', function (e) {
                        if ($tblCompanyOrgEntityDataTable.data().count() > 0) {
                            $($companyOrgEntityTableSelector + ' tbody tr').removeClass('entity-selected');
                            $(this).addClass('entity-selected');
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

        plugin.getSelectedEntity = function () {
            if ($('#tab-entity-chooser-individual' + $elementCount).hasClass('active')) {
                return $tblIndividualEntityDataTable.rows('.entity-selected').data()[0];
            }
            else {
                return $tblCompanyOrgEntityDataTable.rows('.entity-selected').data()[0];
            }
        }

        // fire up the plugin!
        // call the "constructor" method
        plugin.init();

    }

    // add the plugin to the jQuery.fn object
    $.fn.EntityChooser = function (options) {

        // iterate through the DOM elements we are attaching the plugin to
        return this.each(function () {

            // if plugin has not already been attached to the element
            if (undefined === $(this).data('EntityChooser')) {

                // create a new instance of the plugin
                // pass the DOM element and the user-provided options as arguments
                let plugin = new $.EntityChooser(this, options);

                // in the jQuery version of the element
                // store a reference to the plugin object
                // you can later access the plugin and its methods and properties like
                // element.data('EntityChooser').publicMethod(arg1, arg2, ... argn) or
                // element.data('EntityChooser').settings.propertyName
                $(this).data('EntityChooser', plugin);
            }
        });
    }

})(jQuery);
