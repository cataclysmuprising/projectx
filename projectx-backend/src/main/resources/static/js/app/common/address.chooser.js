// https://stackoverflow.com/a/24359224/1531064
// https://jsfiddle.net/nUzJN/286/
(function ($) {

    $.AddressChooser = function (element, options) {
        let $elementCount = "-" + $('.modal-address-chooser').length;
        // Declare UI Template
        const chooseAddressModelTemplate = ({title}) => `
            <div class="modal fade modal-address-chooser" id="modal-address-chooser${$elementCount}">
                <div class="modal-dialog modal-xl modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h4 class="modal-title">${title} <span class="text-muted ml-1 text-xs"></h4>
                            <div class="d-inline-block ml-2 mr-auto">
                                <span class="badge bg-primary" id="title-total-addr-count${$elementCount}">0</span>
                                <span class="text-blue ml-1">Address(es) exist</span>
                            </div>
                            <button aria-hidden="true" class="close" data-dismiss="modal" type="button">&times;</button>
                        </div>
            
                        <div class="modal-body" style="max-height: 450px;overflow: auto;">
                            <div class="col-12">
                                <p class="text-center text-orange mt-1 d-none" id="address-chooser-message-box" style="background-color: #f9ebda;font-size: .75rem;border: 1px dashed;padding: .75rem;">
                                </p>
                            </div>
                        </div>
            
                        <div class="modal-footer text-center">
                            <button class="btn btn-flat btn-success" id="btn-address-chooser-createnew${$elementCount}" type="button"><i class="fas fa-plus-circle"></i> Create New</button>
                            <button class="btn btn-flat btn-secondary" id="btn-address-chooser-select${$elementCount}" type="button"><i class="fas fa-check-square"></i> Select</button>
                            <button class="btn btn-flat btn-danger" data-dismiss="modal" type="button"><i class="fas fa-times"></i> Close</button>
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
            onChooseAddress: function (obj) {
                if (options.onChooseAddress !== undefined) {
                    options.onChooseAddress(plugin, $element, obj);
                }
            },

            onModalShow: function (obj) {
                // initAddressDataTable();
                $modalAddressChooser.modal({
                    backdrop: 'static',
                    keyboard: false
                });
            },

            onModalClose: function (options) {
                $modalAddressChooser.modal("hide");
                // reset form datas
                // hide message box
                $('#address-chooser-message-box').removeClass('d-block').addClass('d-none');
                // clear bankaccount result tables
                // $tblAddressDataTable.clear().destroy();
            }

        }

        // to avoid confusions, use "plugin" to reference the
        // current instance of the object
        let plugin = this;

        // this will hold the merged default, and user-provided options
        // plugin's properties will be available through this object like:
        // plugin.settings.propertyName from inside the plugin or
        // element.data('AddressChooser').settings.propertyName from outside the plugin,
        // where "element" is the element the plugin is attached to;
        plugin.settings = {};
        plugin.searchCriteria = {};
        plugin.addresses = [];
        plugin.addressOwnerId = -99999;
        plugin.selectedAddr = undefined;

        let $element = $(element); // reference to the jQuery version of DOM element

        // Declare functional Elements to variables
        let $modalAddressChooser;
        let $totalAddrCountElem;
        let $btnCreateNewAddress, $btnChooseAddress;
        let $tblAddressTableSelector;

        // the "constructor" method that gets called when the object is created
        plugin.init = function () {
            // the plugin's final properties are the merged default and
            // user-provided options (if any)
            plugin.settings = $.extend({}, events, options);
            if (!plugin.settings.masterdatas) {
                plugin.settings.masterdatas = {};
            }
            $('body').append([{title: plugin.settings.chooseModalTitle}].map(chooseAddressModelTemplate).join(''));

            // add the custom css rules for plugin
            CSS(`
                .background-modal {
                    opacity: 0;
                    z-index: -10 !important;
                }
                .tbl-address, .tbl-beneficiary {
                    cursor: pointer;
                }
                
                .tbl-address.not-selected {
                    opacity: 0.7;
                }
                
                .tbl-address.not-selected:hover {
                    box-shadow: 1px 2px 5px #dbdbdb;
                    opacity: 1;
                    transition: 0.1s all;
                    outline: 1px dashed #c6c6c6;
                }
                
                .tbl-address.selected thead tr th {
                    background-color: #717070;
                    color: #fff;
                }
                
                .tbl-address.selected tbody tr td {
                    background-color: #bdbdbd;
                }
                
                .tbl-address td, .tbl-address th{
                    padding: .75rem !important;
                }
                
                .tbl-address tbody tr td {
                    white-space: break-spaces;
                }
            `);

            // bind variables to DOM elements
            $modalAddressChooser = $('#modal-address-chooser' + $elementCount);
            $totalAddrCountElem = $('#title-total-addr-count' + $elementCount);
            $btnCreateNewAddress = $('btn-address-chooser-createnew' + $elementCount);
            $btnChooseAddress = $('#btn-address-chooser-select' + $elementCount);
            $tblAddressTableSelector = '#tbl-address-result' + $elementCount;

            // modal close event
            $modalAddressChooser.find('.modal-close').on('click', function (e) {
                events.onModalClose(options);
            });

            // bind event for 'select' button event
            $btnChooseAddress.on('click', function (e) {
                if ($modalAddressChooser.find('.tbl-address.selected').length > 0) {
                    let seletedAddressTable = $modalAddressChooser.find('.tbl-address.selected');
                    plugin.selectedAddr = plugin.addresses[parseInt(seletedAddressTable.attr('data-addr-index'))];
                    events.onChooseAddress(plugin.selectedAddr);
                    events.onModalClose(options);
                }
                else {
                    let message = "Select the related entry by <strong>selecting the row</strong> of address results. If there has no-related informations, <strong>click</strong> the <kbd>Create New</kbd> button to add informations.";
                    let messageBox = $('#address-chooser-message-box');
                    messageBox.html(message);
                    messageBox.removeClass('d-none').addClass('d-block');

                }
            });

            // bind event for 'Create New' button event
            if (plugin.settings.enableCreateNew === true) {
                $btnCreateNewAddress = $('#btn-address-chooser-createnew' + $elementCount).AddressCreator({
                    createModalTitle: plugin.settings.createModalTitle,
                    masterdatas: plugin.settings.masterdatas,
                    onModalShow: function (plugin, $element, obj) {
                        // close current chooser modal
                        //events.onModalClose(options);
                        $modalAddressChooser.addClass("background-modal");
                    },
                    onModalClose: function (plugin, $element, obj) {
                        $modalAddressChooser.removeClass("background-modal");
                    },
                    onCreateAddress: function (obj) {
                        plugin.addresses.unshift(obj);
                        plugin.setAddresses(plugin.addressOwnerId, plugin.addresses);
                    },
                });
            }
            else {
                $('#btn-address-chooser-createnew' + $elementCount).remove();
            }

            // add click event to plugin's element
            $element.on("click", function () {
                events.onModalShow(options);
            });
        }

        // private methods
        // these methods can be called only from inside the plugin like:
        // methodName(arg1, arg2, ... argn)

        let getAddressTableElem = function (addrObj) {
            let tableElem = [];
            tableElem.push('<table class="table w-100 table-bordered tbl-address" data-id="' + addrObj.id + '">');
            tableElem.push('<thead><tr><th>Country</th>');

            if (addrObj.country === 'Myanmar') {
                tableElem.push('<th>State/Division</th><th>Township</th><th>Ward/Village</th><th>Postal Code</th><th>Building No#</th><th>Road/Street</th>');
            }
            else {
                tableElem.push('<th>Postal Code</th><th>Address Line 1</th><th>Address Line 1</th>');
            }
            tableElem.push('</tr></thead>');
            tableElem.push('<tbody><tr>');
            tableElem.push('<td>' + addrObj.country + '</td>');
            if (addrObj.country === 'Myanmar') {
                tableElem.push('<td>' + (isNotEmpty(addrObj.firstLevel) ? addrObj.firstLevel : '-') + '</td>');
                tableElem.push('<td>' + (isNotEmpty(addrObj.thirdLevel) ? addrObj.thirdLevel : '-') + '</td>');
                tableElem.push('<td>' + (isNotEmpty(addrObj.fourthLevel) ? addrObj.fourthLevel : '-') + '</td>');
                tableElem.push('<td>' + (isNotEmpty(addrObj.postalCode) ? addrObj.postalCode : '-') + '</td>');
                tableElem.push('<td>' + (isNotEmpty(addrObj.buildingNo) ? addrObj.buildingNo : '-') + '</td>');
                tableElem.push('<td>' + (isNotEmpty(addrObj.streetName) ? addrObj.streetName : '-') + '</td>');
            }
            else {
                tableElem.push('<td>' + (isNotEmpty(addrObj.postalCode) ? addrObj.postalCode : '-') + '</td>');
                tableElem.push('<td>' + (isNotEmpty(addrObj.buildingNo) ? addrObj.buildingNo : '-') + '</td>');
                tableElem.push('<td>' + (isNotEmpty(addrObj.streetName) ? addrObj.streetName : '-') + '</td>');
            }
            tableElem.push('</tr></tbody>');
            tableElem.push('</table>');

            return tableElem.join('');
        }

        // public methods
        // these methods can be called like:
        // plugin.methodName(arg1, arg2, ... argn) from inside the plugin or
        // element.data('pluginName').publicMethod(arg1, arg2, ... argn) from outside, $(this).data('pluginName').publicMethod()
        // the plugin, where "element" is the element the plugin is attached to;

        plugin.setAddresses = function (addressOwnerId, addresses) {
            $modalAddressChooser.find('.modal-body').empty();
            plugin.addressOwnerId = addressOwnerId;
            plugin.addresses = addresses;
            if (addresses && addresses.length > 0) {
                $totalAddrCountElem.text(addresses.length);
            }
            else {
                $modalAddressChooser.find('.modal-body').append('<em class="address-info-msg text-center d-block p-5 text-orange">There hasn\'t any address information available yet.Click the <kbd>Create New</kbd> button to create new address information.</em>');
                $totalAddrCountElem.text(0);
            }

            if (plugin.settings.enableCreateNew === true) {
                $btnCreateNewAddress.data('AddressCreator').setAddressOwnerId(addressOwnerId);
            }

            $(addresses).each(function (index, addrObj) {
                let tblElem = $(getAddressTableElem(addrObj));
                tblElem.attr('data-addr-id', addrObj.id);
                tblElem.attr('data-addr-index', index);
                if (index === 0) {
                    tblElem.addClass('selected')
                }
                else {
                    tblElem.addClass('not-selected')
                }

                if (index + 1 === addresses.length) {
                    tblElem.addClass('mb-0')
                }
                $modalAddressChooser.find('.modal-body').append(tblElem);
            });
            $modalAddressChooser.find('.tbl-address').on('click', function (e) {
                $modalAddressChooser.find('.tbl-address').removeClass('selected').addClass('not-selected');
                $(this).removeClass('not-selected').addClass('selected');
            });
        }

        plugin.getSelectedAddress = function () {
            return plugin.selectedAddr;
        }

        plugin.getTotalAddressCount = function () {
            return plugin.addresses ? plugin.addresses.length : 0;
        }

        plugin.getSelectedAddressElement = function () {
            let tableElem = $($.parseHTML(getAddressTableElem(plugin.selectedAddr)));
            tableElem.removeClass('tbl-address');
            return tableElem;
        }

        // fire up the plugin!
        // call the "constructor" method
        plugin.init();

    }

    // add the plugin to the jQuery.fn object
    $.fn.AddressChooser = function (options) {

        // iterate through the DOM elements we are attaching the plugin to
        return this.each(function () {

            // if plugin has not already been attached to the element
            if (undefined === $(this).data('AddressChooser')) {

                // create a new instance of the plugin
                // pass the DOM element and the user-provided options as arguments
                let plugin = new $.AddressChooser(this, options);

                // in the jQuery version of the element
                // store a reference to the plugin object
                // you can later access the plugin and its methods and properties like
                // element.data('AddressChooser').publicMethod(arg1, arg2, ... argn) or
                // element.data('AddressChooser').settings.propertyName
                $(this).data('AddressChooser', plugin);
            }
        });
    }

})(jQuery);
