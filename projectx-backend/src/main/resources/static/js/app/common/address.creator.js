// https://stackoverflow.com/a/24359224/1531064
// https://jsfiddle.net/nUzJN/286/
(function ($) {

    $.AddressCreator = function (element, options) {
        let $elementCount = "-" + $('.modal-address-creator').length;
        // Declare UI Template
        const createAddressModelTemplate = ({title}) => `
                <div class="modal fade modal-address-creator" id="modal-address-creator${$elementCount}">
                    <div class="modal-dialog modal-xl modal-dialog-centered" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h4 class="modal-title align-self-center">${title}</h4>
                                <button aria-hidden="true" class="close modal-close" type="button">&times;</button>
                            </div>
                            <div class="modal-body">
                                <form id="frm-address-info${$elementCount}">
                                    <div class="form-group address-container mb-0 p-2 p-lg-3" style="padding-top: .5rem !important;">
                                        <form id="frm-address-creator${$elementCount}">
                                            <div class="mb-2 d-flex justify-content-between align-items-center">
                                                <div>
                                                    <label class="mb-0">Address</label>
                                                </div>
                                            </div>
                                            <div class="row">
                                                <div class="col-sm-12 col-md-6 col-lg-3">
                                                    <div class="form-group">
                                                        <select class="dropdown-select country-selectpicker form-control show-tick" data-live-search="true" data-size="5" id="lst-address-creator-country${$elementCount}" title="Select Country"></select>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-md-6 col-lg-3">
                                                    <div class="form-group">
                                                        <select class="dropdown-select state-division-selectpicker form-control show-tick" data-live-search="true" data-size="5" id="lst-address-creator-state-division${$elementCount}" title="Select State/Division"></select>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-md-6 col-lg-3">
                                                    <div class="form-group">
                                                        <select class="dropdown-select township-selectpicker form-control show-tick" data-live-search="true" data-size="5" id="lst-address-creator-township${$elementCount}" disabled title="Select Township"></select>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-md-6 col-lg-3">
                                                    <div class="form-group">
                                                        <select class="dropdown-select ward-village-selectpicker form-control show-tick" data-live-search="true" data-size="5" id="lst-address-creator-ward-village${$elementCount}" disabled title="Select Ward/Village Tract"></select>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="row">
                                                <div class="col-sm-12 col-md-6 col-lg-3">
                                                    <div class="form-group">
                                                        <input class="form-control form-control-sm postal-code-inputmask" id="txt-address-creator-postal-code${$elementCount}" maxlength="12" placeholder="Postal Code" type="text" value=""/>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-md-6 col-lg-3">
                                                    <div class="form-group">
                                                        <input class="form-control txt-building-no form-control-sm" id="txt-address-creator-building-no${$elementCount}" maxlength="200" placeholder="Building No." type="text" value=""/>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-md-6 col-lg-3">
                                                    <div class="form-group">
                                                        <input class="form-control txt-road-street form-control-sm" id="txt-address-creator-road-street${$elementCount}" maxlength="200" placeholder="Road/Street" type="text" value=""/>
                                                    </div>
                                                </div>
                                            </div>
                                        </form>
                                    </div>                                
                                </form>
                            </div>
                            <div class="modal-footer text-center">
                                <button class="btn btn-flat btn-primary" id="btn-address-creator-submit${$elementCount}" type="button"><i class="fas fa-save"></i> Submit</button>
                                <button class="btn btn-flat btn-danger modal-close" type="button"><i class="fas fa-times"></i> Cancel</button>
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
            onCreateAddress: function (obj) {
                plugin.createdAddress = obj;
                if (options.onCreateAddress !== undefined) {
                    options.onCreateAddress(obj);
                }
                this.onModalClose(options);
            },

            onModalShow: function (obj) {
                if (options.onModalShow !== undefined) {
                    options.onModalShow(plugin, $element, obj);
                }
                $modalAddressCreator.modal({
                    backdrop: 'static',
                    keyboard: false
                });
            },

            onModalClose: function (options) {
                $modalAddressCreator.modal("hide");
                // reset form datas
                $frmAddressInfo.trigger('reset');
                $lstAddrCountry.selectpicker('val', 'Myanmar').selectpicker('refresh');
                $lstAddrRegion.prop('disabled', false).selectpicker('val', '').selectpicker('refresh');
                $lstAddrTownship.prop('disabled', true).selectpicker('val', '').selectpicker('refresh');
                $lstAddrWardVillage.prop('disabled', true).selectpicker('val', '').selectpicker('refresh');

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
        // element.data('AddressCreator').settings.propertyName from outside the plugin,
        // where "element" is the element the plugin is attached to;
        plugin.settings = {};
        plugin.addressOwnerId = -99999;
        plugin.createdAddress = undefined;

        let $element = $(element); // reference to the jQuery version of DOM element

        // Declare functional Elements to variables
        let $modalAddressCreator, $frmAddressInfo;
        let $lstAddrCountry, $lstAddrRegion, $lstAddrTownship, $lstAddrWardVillage, $txtAddrPostalCode, $txtAddrBuildingNo, $txtAddrRoadStreet;
        let $btnSubmitNewAddress;

        // the "constructor" method that gets called when the object is created
        plugin
            .init = function () {
            // the plugin's final properties are the merged default and
            // user-provided options (if any)
            plugin.settings = $.extend({}, events, options);
            if (!plugin.settings.masterdatas) {
                plugin.settings.masterdatas = {};
            }

            $('body').append([{title: plugin.settings.createModalTitle}].map(createAddressModelTemplate).join(''));

            // add the custom css rules for plugin
            CSS(`
                .background-modal {
                    opacity: 0;
                    z-index: -10 !important;
                }
            `);

            // bind variables to DOM elements
            $modalAddressCreator = $('#modal-address-creator' + $elementCount);
            $frmAddressInfo = $('#frm-address-info' + $elementCount);
            $btnSubmitNewAddress = $('#btn-address-creator-submit' + $elementCount);

            $lstAddrCountry = $('#lst-address-creator-country' + $elementCount);
            $lstAddrRegion = $('#lst-address-creator-state-division' + $elementCount);
            $lstAddrTownship = $('#lst-address-creator-township' + $elementCount);
            $lstAddrWardVillage = $('#lst-address-creator-ward-village' + $elementCount);
            $txtAddrPostalCode = $('#txt-entity-creator-postal-code' + $elementCount);
            $txtAddrBuildingNo = $('#txt-address-creator-building-no' + $elementCount);
            $txtAddrRoadStreet = $('#txt-address-creator-road-street' + $elementCount);

            // modal close event
            $modalAddressCreator.find('.modal-close').on('click', function (e) {
                events.onModalClose(options);
            });

            // Address Type
            $btnSubmitNewAddress.on('click', function (e) {
                if (plugin.addressOwnerId !== -99999) {
                    let url = getApiResourcePath() + 'sec/address/' + plugin.addressOwnerId + '/create-entity-address';
                    let dto = {
                        buildingNo: $txtAddrBuildingNo.val(),
                        streetName: $txtAddrRoadStreet.val(),
                        firstLevel: $lstAddrRegion.val(),
                        thirdLevel: $lstAddrTownship.val(),
                        fourthLevel: $lstAddrWardVillage.val(),
                        postalCode: $txtAddrPostalCode.val(),
                        country: $lstAddrCountry.val()
                    };

                    $.ajax({
                        type: "POST",
                        data: JSON.stringify(dto),
                        url: url,
                        success: function (response, textStatus, xhr) {
                            if (xhr.status === 200 && response.code === 200) {
                                if (response.createdData) {
                                    events.onCreateAddress(response.createdData);
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
                    alert("Invalid address owner information !");
                }
            });

            // add click event to plugin's element
            $element.on("click", function () {
                events.onModalShow(options);
            });

            // load related master datas
            initDivisions(false);
            initCountries(false);
            $lstAddrCountry.on("change", function (e) {
                let addressContainer = $(this).closest(".address-container");
                if (addressContainer.length === 1 && $(this).val() !== "Myanmar") {
                    addressContainer.find('.state-division-selectpicker,.township-selectpicker,.ward-village-selectpicker').prop("disabled", true).selectpicker('refresh');
                    addressContainer.find('.txt-building-no').prop("placeholder", "Addreess Line 1");
                    addressContainer.find('.txt-road-street').prop("placeholder", "Addreess Line 2");
                }
                else {
                    // state division selectpicker တွေကို enable ပြန်လုပ်မယ်။
                    let stateDivisionSelectpicker = addressContainer.find('.state-division-selectpicker');
                    stateDivisionSelectpicker.prop("disabled", false).selectpicker('refresh');
                    // township နဲ့ ward-village ကိုတော့ အရင်ရွေးထားပြီးသား တန်ဖိုးရှိမှ enable ပြန်လုပ်မယ်။
                    let townshipSelectpicker = addressContainer.find('.township-selectpicker');
                    if (isNotEmpty(townshipSelectpicker.val()) || isNotEmpty(stateDivisionSelectpicker.val())) {
                        townshipSelectpicker.prop("disabled", false).selectpicker('refresh');
                    }
                    let wardVillageSelectpicker = addressContainer.find('.ward-village-selectpicker');
                    if (isNotEmpty(wardVillageSelectpicker.val()) || isNotEmpty(townshipSelectpicker.val())) {
                        wardVillageSelectpicker.prop("disabled", false).selectpicker('refresh');
                    }

                    addressContainer.find('.txt-building-no').prop("placeholder", "Building No.");
                    addressContainer.find('.txt-road-street').prop("placeholder", "Road/Street");
                }
            });

            $modalAddressCreator.find(".address-container .state-division-selectpicker").on("change", function (e) {
                loadTownship($(this).find("option:selected").data("srcode"), $(this).closest('.address-container'));
            });

            $modalAddressCreator.find(".address-container .township-selectpicker").on("change", function (e) {
                loadWardVillage($(this).find("option:selected").data("tspcode"), $(this).closest('.address-container'));
            });
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
        let initCountries = function (isByForce) {
            let criteria = {};
            if (isByForce === false && plugin.settings.masterdatas.countries) {
                let options = [];
                options.push('<option value="" selected="selected">Not selected</option>');
                $.each(plugin.settings.masterdatas.countries, function (key, item) {
                    // TODO: load image from internal not from other site's remote url
                    // let optionItem = "<img class='select-item-img' src='" + item.flagSVG + "'/> " + item.commonName;
                    let optionItem = item.commonName;
                    let option = '<option data-content="' + optionItem + '" value="' + item.commonName + '">' + item.commonName + '</option>';
                    options.push(option);
                });
                setDefaultSelectToSelectPickers($lstAddrCountry, options, "Myanmar");
            }
            else {
                $.ajax({
                    type: "GET",
                    async: false,
                    url: getApiResourcePath() + 'sec/master-data/general/countries',
                    data: criteria,
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            plugin.settings.masterdatas.countries = response;
                            let options = [];
                            options.push('<option value="" selected="selected">Not selected</option>');
                            $.each(response, function (key, item) {
                                // TODO: load image from internal not from other site's remote url
                                // let optionItem = "<img class='select-item-img' src='" + item.flagSVG + "'/> " + item.commonName;
                                let optionItem = item.commonName;
                                let option = '<option data-content="' + optionItem + '" value="' + item.commonName + '">' + item.commonName + '</option>';
                                options.push(option);
                            });
                            setDefaultSelectToSelectPickers($lstAddrCountry, options, "Myanmar");
                        }
                        else {
                            notify("Error", "Failed to fetch country informations.", "error");
                        }
                    }
                });
            }
        }

        let initDivisions = function (isByForce) {
            if (isByForce === false && plugin.settings.masterdatas.divisions) {
                let options = [];
                options.push('<option value="" selected="selected">Not selected</option>');
                $.each(plugin.settings.masterdatas.divisions, function (key, item) {
                    let option = "<option data-srcode = '" + item.stateRegionCode + "' value='" + item.stateRegionDescMM + "'>" + item.stateRegionDescMM + "</option>";
                    options.push(option);
                });
                $lstAddrRegion.html(options).selectpicker('refresh');
            }
            else {
                let criteria = {};
                $.ajax({
                    type: "GET",
                    async: false,
                    url: getApiResourcePath() + 'sec/master-data/general/state-division',
                    data: criteria,
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            plugin.settings.masterdatas.divisions = response;
                            let options = [];
                            options.push('<option value="" selected="selected">Not selected</option>');
                            $.each(response, function (key, item) {
                                let option = "<option data-srcode = '" + item.stateRegionCode + "' value='" + item.stateRegionDescMM + "'>" + item.stateRegionDescMM + "</option>";
                                options.push(option);
                            });
                            $lstAddrRegion.html(options).selectpicker('refresh');
                        }
                        else {
                            notify("Error", "Failed to fetch state-division informations.", "error");
                        }
                    }
                });
            }
        }

        let loadTownship = function (stateDivisionCode, parentContainer) {
            if (isNotEmpty(stateDivisionCode)) {
                let targetElems = parentContainer.find('.township-selectpicker');
                let criteria = {};
                criteria.stateDivisionCode = stateDivisionCode;
                $.ajax({
                    type: "GET",
                    url: getApiResourcePath() + 'sec/master-data/general/township',
                    data: criteria,
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            let options = [];
                            options.push('<option value="" selected="selected">Not selected</option>');
                            $.each(response, function (key, item) {
                                let option = "<option data-tspcode='" + item.townshipCode + "' value='" + item.townshipDescMM + "'>" + item.townshipDescMM + "</option>";
                                options.push(option);
                            });
                            targetElems.html(options).selectpicker('refresh');
                            targetElems.prop("disabled", false).selectpicker('refresh');
                        }
                        else {
                            notify("Error", "Failed to fetch township informations.", "error");
                        }
                    }
                });
            }
        }

        let loadWardVillage = function (townshipCode, parentContainer) {
            if (isNotEmpty(townshipCode)) {
                let targetElems = $(parentContainer).find('.ward-village-selectpicker');
                let criteria = {};
                criteria.townshipCode = townshipCode;
                $.ajax({
                    type: "GET",
                    url: getApiResourcePath() + 'sec/master-data/general/ward-village',
                    data: criteria,
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            let options = [];
                            options.push('<option value="" selected="selected">Not selected</option>');
                            $.each(response, function (key, item) {
                                let option = "<option value='" + item.wardVillageDescMM + "'>" + item.wardVillageDescMM + "</option>";
                                options.push(option);
                            });
                            targetElems.html(options).selectpicker('refresh');
                            targetElems.prop("disabled", false).selectpicker('refresh');
                        }
                        else {
                            notify("Error", "Failed to fetch ward-village informations.", "error");
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
        plugin.setAddressOwnerId = function (addressOwnerId) {
            plugin.addressOwnerId = addressOwnerId;
        }

        plugin.getCreatedAddress = function () {
            return plugin.createdAddress;
        }

        // fire up the plugin!
        // call the "constructor" method
        plugin.init();

    }

    // add the plugin to the jQuery.fn object
    $.fn.AddressCreator = function (options) {

        // iterate through the DOM elements we are attaching the plugin to
        return this.each(function () {

            // if plugin has not already been attached to the element
            if (undefined === $(this).data('AddressCreator')) {

                // create a new instance of the plugin
                // pass the DOM element and the user-provided options as arguments
                let plugin = new $.AddressCreator(this, options);

                // in the jQuery version of the element
                // store a reference to the plugin object
                // you can later access the plugin and its methods and properties like
                // element.data('AddressCreator').publicMethod(arg1, arg2, ... argn) or
                // element.data('AddressCreator').settings.propertyName
                $(this).data('AddressCreator', plugin);
            }
        });
    }

})(jQuery);
