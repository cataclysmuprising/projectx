// https://stackoverflow.com/a/24359224/1531064
// https://jsfiddle.net/nUzJN/286/
(function ($) {

    $.BankAccountCreator = function (element, options) {
        let $elementCount = "-" + $('.modal-bankaccount-creator').length;
        // Declare UI Template
        const createBankAccountModelTemplate = ({title}) => `
                <div class="modal fade modal-bankaccount-creator" id="modal-bankaccount-creator${$elementCount}">
                    <div class="modal-dialog modal-xl modal-dialog-centered" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h4 class="modal-title align-self-center">${title}</h4>
                                <button aria-hidden="true" class="close modal-close" type="button">&times;</button>
                            </div>
                            <div class="modal-body">
                                <form id="frm-bankaccount-info${$elementCount}">
                                    <div class="row">
                                        <div class="col-12 col-md-4">
                                            <div class="form-group">
                                                <label class="required-right" for="txt-bankaccount-creator-bank-accno${$elementCount}">Account Holder No.#</label>
                                                <input class="form-control form-control-sm" id="txt-bankaccount-creator-bank-accno${$elementCount}" placeholder="Enter Account Number" type="text" value="" name="bankAccountCreator.accountNo">
                                            </div>
                                        </div>                                
                                        <div class="col-12 col-md-4">
                                            <div class="form-group">
                                                <label class="required-right" for="txt-bankaccount-creator-accname${$elementCount}">Account Holder Name</label>
                                                <input class="form-control form-control-sm" id="txt-bankaccount-creator-accname${$elementCount}" placeholder="Enter bank account name" type="text" value="" name="bankAccountCreator.accountName">
                                            </div>
                                        </div>
                                         <div class="col-12 col-md-4">
                                            <div class="form-group">
                                                <label for="lst-bankaccount-creator-bankacc-type${$elementCount}">Account Type</label>
                                                <div class="input-group">
                                                    <select class="dropdown-select form-control bankaccount-type-picker show-tick" data-live-search="true" data-size="5" id="lst-bankaccount-creator-bankacc-type${$elementCount}">
                                                    </select>
                                                    <div class="input-group-append">
                                                        <button class="btn btn-default" id="btn-bankaccount-creator-create-new${$elementCount}" type="button"><i class="fas fa-plus-circle"></i> Create New</button>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="row">
                                         <div class="col-12 col-md-4">
                                            <div class="form-group">
                                                <label class="required-right" for="lst-bankaccount-creator-bank${$elementCount}">Bank</label>
                                                <select class="dropdown-select form-control show-tick" data-live-search="true" name="bankAccountCreator.bank" data-size="5" id="lst-bankaccount-creator-bank${$elementCount}" title="Select Bank"></select>
                                            </div>
                                        </div>
                                         <div class="col-12 col-md-4">
                                            <div class="form-group">
                                                <label class="required-right" for="lst-bankaccount-creator-branch${$elementCount}">Branch</label>
                                                <select class="dropdown-select form-control show-tick" data-live-search="true" data-size="5" name="bankAccountCreator.branch" id="lst-bankaccount-creator-branch${$elementCount}" title="Select Branch"></select>
                                            </div>
                                        </div>
                                        <div class="col-12 col-md-4">
                                            <div class="form-group">
                                                <label for="txt-bankaccount-creator-accopen-date">Account Opening Date</label>
                                                <div class="input-group">
                                                    <div class="input-group-prepend">
                                                    <span class="input-group-text">
                                                        <i class="far fa-calendar-alt"></i>
                                                    </span>
                                                    </div>
                                                    <input class="form-control form-control-sm" id="txt-bankaccount-creator-accopen-date${$elementCount}" placeholder="DD-MM-YYYY" type="text" value="">
                                                </div>
                                            </div>
                                        </div>                                    
                                    </div>
                                </form>
                            </div>
                            <div class="modal-footer text-center">
                                <button class="btn btn-flat btn-primary" id="btn-bankaccount-creator-submit${$elementCount}" type="button"><i class="fas fa-save"></i> Submit</button>
                                <button class="btn btn-flat btn-danger modal-close" type="button"><i class="fas fa-times"></i> Cancel</button>
                            </div>
                        </div>
                    </div>
                </div>`;

        const createNewBankAccountTypeModelTemplate = `
                <div class="modal fade" id="modal-bankaccount-creator-create-acctype${$elementCount}">
                    <div class="modal-dialog modal-dialog-centered" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h4 class="modal-title">Create New Bankaccount Type</h4>
                                <button aria-hidden="true" class="close" data-dismiss="modal" type="button">&times;</button>
                            </div>
                            <div class="modal-body">
                                <form id="frm-bankaccount-creator-create-acctype${$elementCount}">
                                    <div class="form-group row">
                                        <div class="col-12">
                                            <input class="form-control form-control-sm" id="txt-bankaccount-creator-acctype-name${$elementCount}" name="acctype-name" placeholder="Enter Bankaccount Type name"/>
                                        </div>
                                    </div>
                                </form>
                            </div>
                            <div class="modal-footer text-center">
                                <button class="btn btn-flat btn-primary" id="btn-bankaccount-creator-save-acctype${$elementCount}" type="button"><i class="fas fa-save"></i> Save</button>
                                <button class="btn btn-flat btn-danger" data-dismiss="modal" type="button"><i class="fas fa-times"></i> Cancel</button>
                            </div>
                        </div>
                    </div>
                </div>        
                `;

        // plugin's default options
        // this is private property and is accessible only from inside the plugin
        let events = {

            // if your plugin is event-driven, you may provide callback capabilities
            // for its events. execute these functions before or after events of your
            // plugin, so that users may customize those particular events without
            // changing the plugin's code
            onCreateBankaccount: function (obj) {
                if (options.onCreateBankaccount !== undefined) {
                    options.onCreateBankaccount(plugin, $element, obj);
                }
                this.onModalClose(options);
            },

            onModalShow: function (obj) {
                if (options.onModalShow !== undefined) {
                    options.onModalShow(plugin, $element, obj);
                }
                $modalBankAccountCreator.modal({
                    backdrop: 'static',
                    keyboard: false
                });
            },

            onModalClose: function (options) {
                $modalBankAccountCreator.modal("hide");
                // reset form datas
                $frmBankaccountInfo.trigger('reset');
                $lstAccountType.selectpicker('val', '').selectpicker('refresh');
                $lstBank.selectpicker('val', $('#hdnCurrentOrgId').val()).selectpicker('refresh').trigger('change');
                $lstBranch.selectpicker('val', $('#hdnCurrentBranchId').val()).selectpicker('refresh');

                removeValidationErrors($modalBankAccountCreator);
                // reset form datas
                if (options.onModalClose !== undefined) {
                    options.onModalClose(plugin, $element, options);
                }
            }

        }

        // to avoid confusions, use "plugin" to reference the
        // current instance of the object
        let plugin = this;

        // this will hold the merged default, and user-provided options
        // plugin's properties will be available through this object like:
        // plugin.settings.propertyName from inside the plugin or
        // element.data('BankaccountCreator').settings.propertyName from outside the plugin,
        // where "element" is the element the plugin is attached to;
        plugin.settings = {};

        let $element = $(element); // reference to the jQuery version of DOM element

        // Declare functional Elements to variables
        let $modalBankAccountCreator, $frmBankaccountInfo;
        let $txtAccountNumber, $txtAccountName, $lstAccountType, $lstBank, $lstBranch, $txtAccountOpeningDate;
        let $btnCreateNewAccountType, $btnSubmitNewBankaccount;

        // the "constructor" method that gets called when the object is created
        plugin
            .init = function () {
            // the plugin's final properties are the merged default and
            // user-provided options (if any)
            plugin.settings = $.extend({}, events, options);
            if (!plugin.settings.masterdatas) {
                plugin.settings.masterdatas = {};
            }

            $('body').append(createNewBankAccountTypeModelTemplate);
            $('body').append([{title: plugin.settings.createModalTitle}].map(createBankAccountModelTemplate).join(''));

            // add the custom css rules for plugin
            CSS(`
                .background-modal {
                    opacity: 0;
                    z-index: -10 !important;
                }
            `);

            // bind variables to DOM elements
            $modalBankAccountCreator = $('#modal-bankaccount-creator' + $elementCount);
            $frmBankaccountInfo = $('#frm-bankaccount-info' + $elementCount);
            $txtAccountNumber = $('#txt-bankaccount-creator-bank-accno' + $elementCount);
            $txtAccountName = $('#txt-bankaccount-creator-accname' + $elementCount);
            $lstAccountType = $('#lst-bankaccount-creator-bankacc-type' + $elementCount);
            $lstBank = $('#lst-bankaccount-creator-bank' + $elementCount);
            $lstBranch = $('#lst-bankaccount-creator-branch' + $elementCount);
            $txtAccountOpeningDate = $('#txt-bankaccount-creator-accopen-date' + $elementCount);
            $txtAccountOpeningDate.inputmask("datetime", {inputFormat: "dd-mm-yyyy", placeholder: "DD-MM-YYYY"});
            $btnCreateNewAccountType = $('#btn-bankaccount-creator-create-new' + $elementCount);
            $btnSubmitNewBankaccount = $('#btn-bankaccount-creator-submit' + $elementCount);

            // modal close event
            $modalBankAccountCreator.find('.modal-close').on('click', function (e) {
                events.onModalClose(options);
            });

            // Bankaccount Type
            $btnCreateNewAccountType.on('click', function (e) {
                $modalBankAccountCreator.addClass("background-modal");
                $("#modal-bankaccount-creator-create-acctype" + $elementCount).modal({
                    backdrop: 'static',
                    keyboard: false
                });
            });

            $("#btn-bankaccount-creator-save-acctype" + $elementCount).on('click', function (e) {
                if (isNotEmpty($('#txt-bankaccount-creator-acctype-name' + $elementCount).val())) {
                    let url = getApiResourcePath() + 'sec/master-data/bank-account-type/create';
                    let dto = {};
                    dto.name = $('#txt-bankaccount-creator-acctype-name' + $elementCount).val();
                    $.ajax({
                        type: "POST",
                        data: JSON.stringify(dto),
                        url: url,
                        success: function (response, textStatus, xhr) {
                            $("#modal-bankaccount-creator-create-acctype" + $elementCount).modal("hide");
                            $('#frm-bankaccount-creator-create-acctype' + $elementCount).trigger("reset");

                            if (xhr.status === 200 && response.code === 200) {
                                if (response.createdData) {
                                    initBankAccountTypes(true, response.createdData.id);
                                }
                                notify(response.title, response.message, "success");
                            }
                            else {
                                notify(response.title, response.message, "error");
                            }
                        },
                    });
                }
                else {
                    alert("Bankaccount Type Name must be present !")
                }
            });

            // show back bankaccount creator modal
            $('#modal-bankaccount-creator-create-acctype' + $elementCount).on("hidden.bs.modal", function () {
                $modalBankAccountCreator.removeClass("background-modal");
            });

            $btnSubmitNewBankaccount.on('click', function (e) {
                if ($frmBankaccountInfo.valid()) {
                    let dto = {
                        accountNo: $txtAccountNumber.val().trim(),
                        accountName: $txtAccountName.val().trim(),
                        accountTypeId: $lstAccountType.val(),
                        bankId: $lstBank.val(),
                        branchId: $lstBranch.val(),
                        accountOpeningDate: $txtAccountOpeningDate.val(),
                        entities: [{id: plugin.settings.ownerId}]
                    };

                    let url = getApiResourcePath() + 'sec/bankAccount/create';
                    $.ajax({
                        type: "POST",
                        data: JSON.stringify(dto),
                        url: url,
                        success: function (response, textStatus, xhr) {
                            if (xhr.status === 200 && response.code === 200) {
                                if (response.createdData) {
                                    events.onCreateBankaccount(response.createdData);
                                }
                                notify(response.title, response.message, "success");
                            }
                            else {
                                notify(response.title, response.message, "error");
                            }
                        },
                    });
                }
            });

            // add click event to plugin's element
            $element.on("click", function () {
                events.onModalShow(options);
            });

            initValidation();
            // load related master datas
            initBankAccountTypes(false);
            initBanks(false);

            $lstBank.on("change", function (e) {
                loadBranches($(this).find("option:selected").data("orgid"));
            });
            $lstBank.selectpicker('val', $('#hdnCurrentOrgId').val()).selectpicker('refresh').trigger('change');
            $lstBranch.selectpicker('val', $('#hdnCurrentBranchId').val()).selectpicker('refresh');
        }

        // private methods
        // these methods can be called only from inside the plugin like:
        // methodName(arg1, arg2, ... argn)
        let setDefaultSelectToSelectPickers = function (targetElems, options, selectedValue) {
            $(targetElems).each(function (index, elem) {
                $(elem).html(options).selectpicker('refresh');
                // အကယ်၍ အရင်ကတစ်ခုခုရွေးထားပြီးသားရှိနေရင် dropdown item list ကို update လုပ်ပြီး အရင်ရွေးထားတဲ့ဟာကို select ပြန်လုပ်ပေးထားမယ်။
                if (isNotEmpty(selectedValue)) {
                    $(elem).selectpicker('val', selectedValue);
                }
            });
        }

        let removeValidationErrors = function (rootContainerElem) {
            rootContainerElem.find('label.error').remove();
            rootContainerElem.find('.error').removeClass('error');
            rootContainerElem.find('.is-invalid').removeClass('is-invalid');
            rootContainerElem.find('.invalid-feedback').remove();
        }

        let initValidation = function () {
            $frmBankaccountInfo.validate({
                rules: {
                    'bankAccountCreator.accountNo': {
                        required: true,
                        maxlength: 20,
                    },
                    'bankAccountCreator.accountName': {
                        required: true,
                        maxlength: 50
                    },
                    'bankAccountCreator.bank': {
                        required: true,
                    },
                    'bankAccountCreator.branch': {
                        required: true,
                    },
                },
                messages: {
                    'bankAccountCreator.accountNo': {
                        required: "`Account Number` field must be present.",
                        maxlength: "`Account Number` may not be greater than (20) characters.",
                    },
                    'bankAccountCreator.accountName': {
                        required: "`Account Name` field must be present.",
                        maxlength: "`Account Name` may not be greater than (50) characters.",
                    },
                    'bankAccountCreator.bank': {
                        required: "`Bank` field must be present.",
                    },
                    'bankAccountCreator.branch': {
                        required: "`Branch` field must be present.",
                    },
                }
            });
        }

        let initBanks = function (isByForce) {
            if (isByForce === false && plugin.settings.masterdatas.banks) {
                let options = [];
                $.each(plugin.settings.masterdatas.banks, function (key, item) {
                    let option = "<option data-orgid = '" + item.id + "' value='" + item.id + "'>" + item.name + "</option>";
                    options.push(option);
                });
                $lstBank.html(options).selectpicker('refresh');
            }
            else {
                let criteria = {};
                $.ajax({
                    type: "GET",
                    async: false,
                    url: getApiResourcePath() + 'sec/master-data/general/banks',
                    data: criteria,
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            plugin.settings.masterdatas.banks = response;
                            let options = [];
                            $.each(response, function (key, item) {
                                let option = "<option data-orgid = '" + item.id + "' value='" + item.id + "'>" + item.name + "</option>";
                                options.push(option);
                            });
                            $lstBank.html(options).selectpicker('refresh');
                        }
                        else {
                            notify("Error", "Failed to fetch bank informations.", "error");
                        }
                    }
                });
            }
        }

        let loadBranches = function (organizationId) {
            if (isNotEmpty(organizationId)) {
                let criteria = {};
                criteria.organizationId = organizationId;
                $.ajax({
                    type: "GET",
                    async: false,
                    url: getApiResourcePath() + 'sec/master-data/general/branches',
                    data: criteria,
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            let options = [];
                            $.each(response, function (key, item) {
                                let option = "<option value='" + item.id + "'>" + item.name + "</option>";
                                options.push(option);
                            });
                            $lstBranch.html(options).selectpicker('refresh');
                        }
                        else {
                            notify("Error", "Failed to fetch branch informations.", "error");
                        }
                    }
                });
            }
        }

        let initBankAccountTypes = function (isByForce, selectedValue) {
            if (isByForce === false && plugin.settings.masterdatas.bankaccountTypes) {
                let options = [];
                options.push('<option value="" selected="selected">Not selected</option>');
                $.each(plugin.settings.masterdatas.bankaccountTypes, function (key, item) {
                    let option = '<option value="' + item.id + '">' + item.name + '</option>';
                    options.push(option);
                });
                setDefaultSelectToSelectPickers($lstAccountType, options, selectedValue);
            }
            else {
                let criteria = {};
                $.ajax({
                    type: "POST",
                    async: false,
                    url: getApiResourcePath() + 'sec/master-data/bank-account-type/search/list',
                    data: JSON.stringify(criteria),
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            plugin.settings.masterdatas.bankaccountTypes = response;
                            let options = [];
                            options.push('<option value="" selected="selected">Not selected</option>');
                            $.each(response, function (key, item) {
                                let option = '<option value="' + item.id + '">' + item.name + '</option>';
                                options.push(option);
                            });
                            // အသစ်ထည့်လိုက်တာဆိုရင် picker မှန်သမျှလိုက်ပြီး update လုပ်ဖို့လိုတယ်။
                            if (isByForce === true) {
                                $('.bankaccount-type-picker').selectpicker('destroy').html(options).selectpicker('refresh').selectpicker('render');
                                $lstAccountType.selectpicker('val', selectedValue).selectpicker('refresh');
                            }
                            else {
                                setDefaultSelectToSelectPickers($lstAccountType, options, selectedValue);
                            }
                        }
                        else {
                            notify("Error", "Failed to fetch bankaccount type informations.", "error");
                        }
                    }
                });
            }
        }

        // public methods
        // these methods can be called like:
        // plugin.methodName(arg1, arg2, ... argn) from inside the plugin or
        // element.data('pluginName').publicMethod(arg1, arg2, ... argn) from outside, $(this).data('pluginName').publicMethod()
        // the plugin, where "element" is the element the plugin is attached to;

        // fire up the plugin!
        // call the "constructor" method
        plugin.init();

    }

    // add the plugin to the jQuery.fn object
    $.fn.BankAccountCreator = function (options) {

        // iterate through the DOM elements we are attaching the plugin to
        return this.each(function () {

            // if plugin has not already been attached to the element
            if (undefined === $(this).data('BankAccountCreator')) {

                // create a new instance of the plugin
                // pass the DOM element and the user-provided options as arguments
                let plugin = new $.BankAccountCreator(this, options);

                // in the jQuery version of the element
                // store a reference to the plugin object
                // you can later access the plugin and its methods and properties like
                // element.data('BankAccountCreator').publicMethod(arg1, arg2, ... argn) or
                // element.data('BankAccountCreator').settings.propertyName
                $(this).data('BankAccountCreator', plugin);
            }
        });
    }

})(jQuery);
