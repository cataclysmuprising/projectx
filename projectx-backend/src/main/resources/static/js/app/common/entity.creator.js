// https://stackoverflow.com/a/24359224/1531064
// https://jsfiddle.net/nUzJN/286/
(function ($) {
    $.EntityCreator = function (element, options) {
        let $elementCount = "-" + $('.modal-entity-creator').length;
        // Declare UI Template
        const createEntityModelTemplate = ({title}) => `
                <div class="modal fade modal-entity-creator" id="modal-entity-creator${$elementCount}">
                    <div class="modal-dialog modal-xl modal-dialog-centered" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h4 class="modal-title align-self-center">${title}</h4>
                                <ul class="nav nav-pills ml-3" id="entity-creator-type-menu${$elementCount}">
                                    <li class="nav-item">
                                        <a class="nav-link active" data-toggle="tab" href="#tab-entity-creator-individual${$elementCount}">Individual</a>
                                    </li>
                                    <li class="nav-item">
                                        <a class="nav-link" data-toggle="tab" href="#tab-entity-creator-company-org${$elementCount}">Company/Organization</a>
                                    </li>
                                </ul>
                                 <button aria-hidden="true" class="close modal-close" type="button">&times;</button>
                            </div>
                            <div class="modal-body">
                                <div class="tab-content">
                                    <div class="tab-pane active" id="tab-entity-creator-individual${$elementCount}">
                                        <form id="frm-entity-creator-individual${$elementCount}">
                                            <div class="row filter-container">
                                                <div class="col-12 col-md-6 col-lg-6 nrc-container">
                                                    <label for="lst-entity-creator-nrc-region${$elementCount}">NRC</label>
                                                    <div class="row">
                                                        <div class="col-6 col-lg-3">
                                                            <div class="form-group">
                                                                <div class="input-group">
                                                                    <select class="dropdown-select nrc-state-selectpicker form-control show-tick" data-size="5" id="lst-entity-creator-nrc-region${$elementCount}" title="Region"></select>
                                                                    <div class="input-group-append">
                                                                        <span class="input-group-text pl-3 pr-3">/</span>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="col-6 col-lg-3">
                                                            <div class="form-group">
                                                                <select class="dropdown-select nrc-township-selectpicker form-control show-tick" disabled data-live-search="true" data-size="5" id="lst-entity-creator-nrc-township${$elementCount}" title="Township"></select>
                                                            </div>
                                                        </div>
                                                        <div class="col-6 col-lg-2">
                                                            <div class="form-group">
                                                                <select class="dropdown-select nrc-type-selectpicker form-control show-tick" data-size="6" id="lst-entity-creator-nrc-type${$elementCount}" title="Type">
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
                                                                    <input class="form-control form-control-sm number-only-input" id="txt-entity-creator-nrc-number${$elementCount}" maxlength="6" name="entityCreator.nrcNo" placeholder="NRC Number" type="text" value=""/>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-md-6">
                                                    <div class="form-group">
                                                        <label for="txt-entity-creator-passport-number${$elementCount}">Passport No
                                                            <small class="text-muted text-nowrap">(NRC အချက်အလက် မပါရှိပါက မဖြစ်မနေ ဖြည့်သွင်းရပါမည်။)</small>
                                                        </label>
                                                        <div class="input-group">
                                                            <input class="form-control form-control-sm" id="txt-entity-creator-passport-number${$elementCount}" maxlength="20" name="entityCreator.passportNo" placeholder="Enter passport number" type="text" value=""/>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="row">
                                                <div class="col-sm-12 col-lg-4">
                                                    <div class="form-group">
                                                        <label class="required-right" for="txt-entity-creator-individual-name${$elementCount}">Name</label>
                                                        <input class="form-control form-control-sm" id="txt-entity-creator-individual-name${$elementCount}" maxlength="100" name="entityCreator.individualName" placeholder="Enter name" type="text" value=""/>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-lg-4">
                                                    <div class="form-group">
                                                        <label class="required-right" for="txt-entity-creator-father-name${$elementCount}">Father Name</label>
                                                        <input class="form-control form-control-sm" id="txt-entity-creator-father-name${$elementCount}" maxlength="100" name="entityCreator.individualFatherName" placeholder="Enter father name" type="text" value=""/>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-lg-4">
                                                    <div class="form-group">
                                                        <label class="required-right" for="txt-entity-creator-individual-phone-number${$elementCount}">Phone No.</label>
                                                        <div class="input-group">
                                                            <div class="input-group-prepend">
                                                                <span class="input-group-text"><i class="fas fa-phone"></i></span>
                                                            </div>
                                                            <input class="form-control form-control-sm" id="txt-entity-creator-individual-phone-number${$elementCount}" minlength="7" maxlength="15" name="entityCreator.individualPhoneNo" placeholder="Enter phone number" type="text" value=""/>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                
                                            <div class="row">
                                               <div class="col-sm-12 col-lg-4">
                                                    <div class="form-group">
                                                        <label class="required-right" for="txt-entity-creator-dob${$elementCount}">DOB</label>
                                                        <div class="input-group">
                                                            <div class="input-group-prepend">
                                                                    <span class="input-group-text">
                                                                        <i class="far fa-calendar-alt"></i>
                                                                    </span>
                                                            </div>
                                                            <input class="form-control form-control-sm" placeholder="DD-MM-YYYY" id="txt-entity-creator-dob${$elementCount}" name="entityCreator.individualDob" type="text" value=""/>
                                                        </div>
                                                    </div>                                    
                                               </div>
                                               <div class="col-sm-12 col-lg-4">
                                                   <div class="form-group">
                                                       <label class="required-right" for="lst-entity-creator-occupation${$elementCount}">Occupation</label>
                                                       <div class="input-group">
                                                           <select class="dropdown-select form-control show-tick occupation-picker" data-live-search="true" data-size="5" id="lst-entity-creator-occupation${$elementCount}" name="entityCreator.individualOccupation" title="Select occupation"></select>
                                                           <div class="input-group-append">
                                                               <button class="btn btn-default" id="btn-entity-creator-addnew-occupation${$elementCount}" type="button"><i class="fas fa-plus-circle"></i> Create New</button>
                                                           </div>
                                                       </div>
                                                   </div>
                                               </div>
                                               <div class="col-sm-12 col-lg-4">
                                                   <div class="form-group">
                                                       <label class="d-block">Gender</label>
                                                       <div class="custom-control custom-radio d-inline mr-3">
                                                           <input checked="" class="custom-control-input" id="rdo-entity-creator-gender-male${$elementCount}" name="rdo-entity-creator-gender${$elementCount}" type="radio" value="MALE">
                                                           <label class="custom-control-label" for="rdo-entity-creator-gender-male${$elementCount}">Male</label>
                                                       </div>
                                                       <div class="custom-control custom-radio d-inline">
                                                           <input class="custom-control-input" id="rdo-entity-creator-gender-female${$elementCount}" name="rdo-entity-creator-gender${$elementCount}" type="radio" value="FEMALE">
                                                           <label class="custom-control-label" for="rdo-entity-creator-gender-female${$elementCount}">Female</label>
                                                       </div>
                                                   </div>
                                               </div>
                                            </div>
                        
                                            <div class="row nationality-container">
                                                <div class="col-sm-12 col-md-6 col-lg-4">
                                                    <div class="form-group">
                                                        <label for="lst-entity-creator-citizenship${$elementCount}">Citizenship</label>
                                                        <select class="dropdown-select nationality-selectpicker form-control show-tick" data-live-search="true" data-size="5" id="lst-entity-creator-citizenship${$elementCount}" title="Select Citizenship"></select>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-md-6 col-lg-4">
                                                    <div class="form-group">
                                                        <label for="lst-entity-creator-race${$elementCount}">Race</label>
                                                        <select class="dropdown-select race-selectpicker form-control show-tick" data-size="5" id="lst-entity-creator-race${$elementCount}" title="Select Race">
                                                            <option data-subtext="(ကချင်)" value="Kachin">Kachin</option>
                                                            <option data-subtext="(ကယား)" value="Kayah">Kayah</option>
                                                            <option data-subtext="(ကရင်)" value="Karen">Karen</option>
                                                            <option data-subtext="(ချင်း)" value="Chin">Chin</option>
                                                            <option data-subtext="(ဗမာ)" value="Bamar">Bamar</option>
                                                            <option data-subtext="(မွန်)" value="Mon">Mon</option>
                                                            <option data-subtext="(ရခိုင်)" value="Rakhine">Rakhine</option>
                                                            <option data-subtext="(ရှမ်း)" value="Shan">Shan</option>
                                                        </select>
                                                    </div>
                                                </div>
                                            </div>   
                                        </form>                                 
                                    </div>
                                    <div class="tab-pane" id="tab-entity-creator-company-org${$elementCount}">
                                        <form id="frm-entity-creator-company-org${$elementCount}">
                                            <div class="row">
                                                <div class="col-sm-12 col-lg-4">
                                                    <div class="form-group">
                                                        <label class="required-right" for="txt-entity-creator-regno${$elementCount}">Registeration No.</label>
                                                        <input class="form-control form-control-sm" id="txt-entity-creator-regno${$elementCount}" maxlength="20" name="entityCreator.regNo" placeholder="Enter Registeration No.#" type="text" value=""/>
                                                    </div>
                                                </div>                                        
                                                <div class="col-sm-12 col-lg-4">
                                                    <div class="form-group">
                                                        <label class="required-right" for="txt-entity-creator-company-org-name${$elementCount}">Company Name</label>
                                                        <input class="form-control form-control-sm" id="txt-entity-creator-company-org-name${$elementCount}" name="entityCreator.companyOrgName" maxlength="100" placeholder="Enter company name" type="text" value=""/>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-lg-4">
                                                    <div class="form-group">
                                                        <label for="lst-entity-creator-business-type${$elementCount}">Business Type</label>
                                                        <div class="input-group">
                                                            <select class="dropdown-select form-control show-tick business-type-picker" data-live-search="true" data-size="5" id="lst-entity-creator-business-type${$elementCount}" title="Select business type">
                                                            </select>
                                                            <div class="input-group-append">
                                                                <button class="btn btn-default" id="btn-entity-creator-addnew-business-type${$elementCount}" type="button"><i class="fas fa-plus-circle"></i> Create New</button>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                    
                                            <div class="row">
                                                <div class="col-sm-12 col-md-6 col-lg-4">
                                                    <div class="form-group">
                                                        <label for="txt-entity-creator-company-org-phone-no${$elementCount}">Phone No.</label>
                                                        <div class="input-group">
                                                            <div class="input-group-prepend">
                                                                <span class="input-group-text"><i class="fas fa-phone"></i></span>
                                                            </div>
                                                            <input class="form-control form-control-sm" id="txt-entity-creator-company-org-phone-no${$elementCount}" placeholder="Enter phone number" type="text" value=""/>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="col-sm-12 col-md-6 col-lg-4">
                                                    <div class="form-group">
                                                        <label for="lst-entity-creator-reg-country${$elementCount}">Registeration Country</label>
                                                        <select class="dropdown-select form-control show-tick" data-live-search="true" data-size="5" id="lst-entity-creator-reg-country${$elementCount}" title="Select registeration country">
                                                        </select>
                                                    </div>
                                                </div>
                                            </div> 
                                        </form>                                   
                                    </div>
                                </div>
                                
                                <div class="form-group address-container mb-0 p-2 p-lg-3" style="padding-top: .5rem !important;">
                                    <form id="frm-entity-creator-address${$elementCount}">
                                        <div class="mb-2 d-flex justify-content-between align-items-center">
                                            <div>
                                                <label class="mb-0">Address</label>
                                            </div>
                                        </div>
                                        <div class="row">
                                            <div class="col-sm-12 col-md-6 col-lg-3">
                                                <div class="form-group">
                                                    <select class="dropdown-select country-selectpicker form-control show-tick" data-live-search="true" data-size="5" id="lst-entity-creator-address-country${$elementCount}" title="Select Country"></select>
                                                </div>
                                            </div>
                                            <div class="col-sm-12 col-md-6 col-lg-3">
                                                <div class="form-group">
                                                    <select class="dropdown-select state-division-selectpicker form-control show-tick" data-live-search="true" data-size="5" id="lst-entity-creator-address-state-division${$elementCount}" title="Select State/Division"></select>
                                                </div>
                                            </div>
                                            <div class="col-sm-12 col-md-6 col-lg-3">
                                                <div class="form-group">
                                                    <select class="dropdown-select township-selectpicker form-control show-tick" data-live-search="true" data-size="5" id="lst-entity-creator-address-township${$elementCount}" disabled title="Select Township"></select>
                                                </div>
                                            </div>
                                            <div class="col-sm-12 col-md-6 col-lg-3">
                                                <div class="form-group">
                                                    <select class="dropdown-select ward-village-selectpicker form-control show-tick" data-live-search="true" data-size="5" id="lst-entity-creator-address-ward-village${$elementCount}" disabled title="Select Ward/Village"></select>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="row">
                                            <div class="col-sm-12 col-md-6 col-lg-3">
                                                <div class="form-group">
                                                    <input class="form-control form-control-sm postal-code-inputmask" id="txt-entity-creator-postal-code${$elementCount}" maxlength="12" placeholder="Postal Code" type="text" value=""/>
                                                </div>
                                            </div>
                                            <div class="col-sm-12 col-md-6 col-lg-3">
                                                <div class="form-group">
                                                    <input class="form-control txt-building-no form-control-sm" id="txt-entity-creator-address-building-no${$elementCount}" maxlength="200" placeholder="Building No." type="text" value=""/>
                                                </div>
                                            </div>
                                            <div class="col-sm-12 col-md-6 col-lg-3">
                                                <div class="form-group">
                                                    <input class="form-control txt-road-street form-control-sm" id="txt-entity-creator-address-road-street${$elementCount}" maxlength="200" placeholder="Road/Street" type="text" value=""/>
                                                </div>
                                            </div>
                                        </div>
                                    </form>
                                </div>
                            </div>
                
                            <div class="modal-footer text-center">
                                <button class="btn btn-flat btn-primary" id="btn-create-entity-submit${$elementCount}" type="button"><i class="fas fa-save"></i> Submit</button>
                                <button class="btn btn-flat btn-danger modal-close" type="button"><i class="fas fa-times"></i> Cancel</button>
                            </div>
                        </div>
                    </div>
                </div>`;

        const createNewOccupationModelTemplate = `
                <div class="modal fade" id="modal-entity-creator-create-occupation${$elementCount}">
                    <div class="modal-dialog modal-dialog-centered" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h4 class="modal-title">Create New Occupation</h4>
                                <button aria-hidden="true" class="close" data-dismiss="modal" type="button">&times;</button>
                            </div>
                            <div class="modal-body">
                                <form id="frm-entity-creator-create-occupation${$elementCount}">
                                    <div class="form-group row">
                                        <div class="col-12">
                                            <input class="form-control form-control-sm" id="txt-entity-creator-occupation-name${$elementCount}" name="occupation-name" placeholder="Enter occupation name"/>
                                        </div>
                                    </div>
                                </form>
                            </div>
                            <div class="modal-footer text-center">
                                <button class="btn btn-flat btn-primary" id="btn-entity-creator-save-occupation${$elementCount}" type="button"><i class="fas fa-save"></i> Save</button>
                                <button class="btn btn-flat btn-danger" data-dismiss="modal" type="button"><i class="fas fa-times"></i> Cancel</button>
                            </div>
                        </div>
                    </div>
                </div>        
                `;

        const createNewBusinessModelTemplate = `
                <div class="modal fade" id="modal-entity-creator-create-business-type${$elementCount}">
                    <div class="modal-dialog modal-dialog-centered" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h4 class="modal-title">Create New Business Type</h4>
                                <button aria-hidden="true" class="close" data-dismiss="modal" type="button">&times;</button>
                            </div>
                            <div class="modal-body">
                                <form id="frm-entity-creator-create-business-type${$elementCount}">
                                    <div class="form-group row">
                                        <div class="col-12">
                                            <input class="form-control form-control-sm" id="txt-entity-creator-business-type-name${$elementCount}" name="business-type-name" placeholder="Enter business type"/>
                                        </div>
                                    </div>
                                </form>
                            </div>
                            <div class="modal-footer text-center">
                                <button class="btn btn-flat btn-primary" id="btn-entity-creator-save-business-type${$elementCount}" type="button"><i class="fas fa-save"></i> Save</button>
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
            onCreateEntity: function (obj) {
                if (options.onCreateEntity !== undefined) {
                    options.onCreateEntity(obj);
                }
                this.onModalClose(options);
            },

            onModalShow: function (obj) {
                if (options.onModalShow !== undefined) {
                    options.onModalShow(plugin, $element, obj);
                }
                if (plugin.settings.isIndividualEntityOnly === true) {
                    $entityTypeMenu.hide();
                }
                $modalEntityCreator.modal({
                    backdrop: 'static',
                    keyboard: false
                });
            },

            onModalClose: function (options) {
                $modalEntityCreator.modal("hide");
                // reset form datas
                $frmIndividualInfo.trigger('reset');
                $frmCompanyOrgInfo.trigger('reset');
                $frmAddressInfo.trigger('reset');
                $lstNrcRegionCode.selectpicker('val', '').selectpicker('refresh');
                $lstNrcType.selectpicker('val', 'နိုင်').selectpicker('refresh');
                $lstNrcTownshipCode.prop('disabled', true).selectpicker('val', '').selectpicker('refresh');
                $lstOccupation.selectpicker('val', '').selectpicker('refresh');
                $lstCitizenship.selectpicker('val', 'Burmese').selectpicker('refresh');
                $lstRace.prop('disabled', false).selectpicker('val', '').selectpicker('refresh');
                $lstAddrCountry.selectpicker('val', '').selectpicker('refresh');
                $lstAddrRegion.prop('disabled', false).selectpicker('val', '').selectpicker('refresh');
                $lstAddrTownship.prop('disabled', true).selectpicker('val', '').selectpicker('refresh');
                $lstAddrWardVillage.prop('disabled', true).selectpicker('val', '').selectpicker('refresh');
                $lstBusinessType.selectpicker('val', '').selectpicker('refresh');
                $lstRegCountry.selectpicker('val', 'Myanmar').selectpicker('refresh');

                removeValidationErrors($modalEntityCreator);
                // re-select the 'Individual' Tab
                $('.nav-link[href="#tab-entity-creator-individual' + $elementCount + '"]').trigger('click');

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
        // element.data('EntityCreator').settings.propertyName from outside the plugin,
        // where "element" is the element the plugin is attached to;
        plugin.settings = {};
        plugin.newEntityObj = {};

        let $element = $(element); // reference to the jQuery version of DOM element

        // Declare functional Elements to variables
        let $modalEntityCreator;
        let $entityTypeMenu;
        let $nrcContainerElem;
        let $frmIndividualInfo, $frmCompanyOrgInfo, $frmAddressInfo;
        let $lstNrcRegionCode, $lstNrcTownshipCode, $lstNrcType, $txtNrcNo, $txtPassportNo, $txtIndividualName, $txtFatherName, $txtIndividualPhone, $txtDOB, $lstOccupation, $rdoGender, $lstCitizenship, $lstRace;
        let $txtRegNo, $txtCompanyOrgName, $lstBusinessType, $txtCompanyOrgPhone, $lstRegCountry;
        let $lstAddrCountry, $lstAddrRegion, $lstAddrTownship, $lstAddrWardVillage, $txtAddrPostalCode, $txtAddrBuildingNo, $txtAddrRoadStreet;
        let $btnCreateEntitySubmit;

        let $tblIndividualEntityDataTable, $individualEntityTableSelector, $tblCompanyOrgEntityDataTable, $companyOrgEntityTableSelector;

        // the "constructor" method that gets called when the object is created
        plugin.init = function () {

            // the plugin's final properties are the merged default and
            // user-provided options (if any)
            plugin.settings = $.extend({}, events, options);
            if (!plugin.settings.masterdatas) {
                plugin.settings.masterdatas = {};
            }
            $('body').append(createNewOccupationModelTemplate);
            if (plugin.settings.isIndividualEntityOnly !== true) {
                $('body').append(createNewBusinessModelTemplate);
            }
            $('body').append([{title: plugin.settings.createModalTitle}].map(createEntityModelTemplate).join(''));

            // add the custom css rules for plugin
            CSS(`
                .background-modal {
                    opacity: 0;
                    z-index: -10 !important;
                }
            `);

            // bind variables to DOM elements
            $frmIndividualInfo = $('#frm-entity-creator-individual' + $elementCount);
            $frmCompanyOrgInfo = $('#frm-entity-creator-company-org' + $elementCount);
            $frmAddressInfo = $('#frm-entity-creator-address' + $elementCount);

            $modalEntityCreator = $('#modal-entity-creator' + $elementCount);
            $entityTypeMenu = $('#entity-creator-type-menu' + $elementCount);
            $nrcContainerElem = $('#tab-entity-creator-individual' + $elementCount).find('.nrc-container');
            $lstNrcRegionCode = $nrcContainerElem.find('.nrc-state-selectpicker');
            $lstNrcTownshipCode = $nrcContainerElem.find('.nrc-township-selectpicker');
            $lstNrcType = $nrcContainerElem.find('.nrc-type-selectpicker');
            $txtNrcNo = $('#txt-entity-creator-nrc-number' + $elementCount);
            $txtPassportNo = $('#txt-entity-creator-passport-number' + $elementCount);
            $txtIndividualName = $('#txt-entity-creator-individual-name' + $elementCount);
            $txtFatherName = $('#txt-entity-creator-father-name' + $elementCount);
            $txtIndividualPhone = $('#txt-entity-creator-individual-phone-number' + $elementCount);
            $txtDOB = $('#txt-entity-creator-dob' + $elementCount);
            $txtDOB.inputmask("datetime", {inputFormat: "dd-mm-yyyy", placeholder: "DD-MM-YYYY"});
            $lstOccupation = $('#lst-entity-creator-occupation' + $elementCount);
            $rdoGender = $('#tab-entity-creator-individual' + $elementCount).find('input[type=radio][name="rdo-entity-creator-gender"]');
            $lstCitizenship = $('#lst-entity-creator-citizenship' + $elementCount);
            $lstRace = $('#lst-entity-creator-race' + $elementCount);

            $txtRegNo = $('#txt-entity-creator-regno' + $elementCount);
            $txtCompanyOrgName = $('#txt-entity-creator-company-org-name' + $elementCount);
            $lstBusinessType = $('#lst-entity-creator-business-type' + $elementCount);
            $txtCompanyOrgPhone = $('#txt-entity-creator-company-org-phone-no' + $elementCount);
            $lstRegCountry = $('#lst-entity-creator-reg-country' + $elementCount);

            $lstAddrCountry = $('#lst-entity-creator-address-country' + $elementCount);
            $lstAddrRegion = $('#lst-entity-creator-address-state-division' + $elementCount);
            $lstAddrTownship = $('#lst-entity-creator-address-township' + $elementCount);
            $lstAddrWardVillage = $('#lst-entity-creator-address-ward-village' + $elementCount);
            $txtAddrPostalCode = $('#txt-entity-creator-postal-code' + $elementCount);
            $txtAddrBuildingNo = $('#txt-entity-creator-address-building-no' + $elementCount);
            $txtAddrRoadStreet = $('#txt-entity-creator-address-road-street' + $elementCount);

            $btnCreateEntitySubmit = $('#btn-create-entity-submit' + $elementCount);

            if ($.fn.inputmask) {
                $(".number-only-input").inputmask({
                    regex: "\\d+",
                    'placeholder': ''
                });
            }

            // modal close event
            $modalEntityCreator.find('.modal-close').on('click', function (e) {
                events.onModalClose(options);
            });

            // Occupation
            $('#btn-entity-creator-addnew-occupation' + $elementCount).on('click', function (e) {
                $modalEntityCreator.addClass("background-modal");
                $("#modal-entity-creator-create-occupation" + $elementCount).modal({
                    backdrop: 'static',
                    keyboard: false
                });
            });

            $("#btn-entity-creator-save-occupation" + $elementCount).on('click', function (e) {
                if (isNotEmpty($('#txt-entity-creator-occupation-name' + $elementCount).val())) {
                    let url = getApiResourcePath() + 'sec/master-data/occupation/create';
                    let dto = {};
                    dto.name = $('#txt-entity-creator-occupation-name' + $elementCount).val();
                    $.ajax({
                        type: "POST",
                        data: JSON.stringify(dto),
                        url: url,
                        success: function (response, textStatus, xhr) {
                            $("#modal-entity-creator-create-occupation" + $elementCount).modal("hide");
                            $('#frm-entity-creator-create-occupation' + $elementCount).trigger("reset");

                            if (xhr.status === 200 && response.code === 200) {
                                if (response.createdData) {
                                    initOccupations(true, response.createdData.id);
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
                    alert("Occupation Name must be present !")
                }
            });

            // Business Type
            if (plugin.settings.isIndividualEntityOnly !== true) {
                $('#btn-entity-creator-addnew-business-type' + $elementCount).on('click', function (e) {
                    $modalEntityCreator.addClass("background-modal");
                    $("#modal-entity-creator-create-business-type" + $elementCount).modal({
                        backdrop: 'static',
                        keyboard: false
                    });
                });

                $("#btn-entity-creator-save-business-type" + $elementCount).on('click', function (e) {
                    if (isNotEmpty($('#txt-entity-creator-business-type-name' + $elementCount).val())) {
                        let url = getApiResourcePath() + 'sec/master-data/business-type/create';
                        let dto = {};
                        dto.name = $('#txt-entity-creator-business-type-name' + $elementCount).val();
                        $.ajax({
                            type: "POST",
                            data: JSON.stringify(dto),
                            url: url,
                            success: function (response, textStatus, xhr) {
                                $("#modal-entity-creator-create-business-type" + $elementCount).modal("hide");
                                $('#frm-entity-creator-create-business-type' + $elementCount).trigger("reset");

                                if (xhr.status === 200 && response.code === 200) {
                                    if (response.createdData) {
                                        initBusinessTypes(true, response.createdData.id);
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
                        alert("Business Type Name must be present !")
                    }
                });
            }

            // show back entity creator modal
            $('#modal-entity-creator-create-occupation' + $elementCount + ',#modal-entity-creator-create-business-type' + $elementCount + '').on("hidden.bs.modal", function () {
                $modalEntityCreator.removeClass("background-modal");
            });

            $btnCreateEntitySubmit.on('click', function (e) {
                if ($('.nav-link[href="#tab-entity-creator-individual' + $elementCount + '"]').hasClass('active') && $frmIndividualInfo.valid()) {
                    let addressDTO = undefined;
                    if (isNotEmpty($txtAddrBuildingNo.val()) || isNotEmpty($txtAddrRoadStreet.val()) || isNotEmpty($lstAddrRegion.val())
                        || isNotEmpty($lstAddrTownship.val()) || isNotEmpty($lstAddrWardVillage.val()) || isNotEmpty($txtAddrPostalCode.val())
                        || isNotEmpty($lstAddrCountry.val())) {
                        addressDTO = {
                            buildingNo: $txtAddrBuildingNo.val().trim(),
                            streetName: $txtAddrRoadStreet.val().trim(),
                            firstLevel: $lstAddrRegion.val(),
                            thirdLevel: $lstAddrTownship.val(),
                            fourthLevel: $lstAddrWardVillage.val(),
                            postalCode: $txtAddrPostalCode.val().trim(),
                            country: $lstAddrCountry.val()
                        };
                    }
                    let dto = {
                        customerType: 'INDIVIDUAL',
                        nrcRegionCode: $lstNrcRegionCode.val(),
                        nrcTownshipCode: $lstNrcTownshipCode.val(),
                        nrcType: $lstNrcType.val(),
                        nrcNo: $txtNrcNo.val().trim(),
                        passportNo: $txtPassportNo.val(),
                        name: $txtIndividualName.val().trim(),
                        fatherName: $txtFatherName.val().trim(),
                        phone: $txtIndividualPhone.val().trim(),
                        dob: $txtDOB.val().trim(),
                        occupationId: $lstOccupation.val(),
                        gender: $('input[name="rdo-entity-creator-gender' + $elementCount + '"]:checked').val(),
                        citizenship: $lstCitizenship.val(),
                        race: $lstRace.val(),
                        address: addressDTO,
                    };

                    createNewEntity(dto);
                }
                if (plugin.settings.isIndividualEntityOnly !== true) {
                    if ($('.nav-link[href="#tab-entity-creator-company-org' + $elementCount + '"]').hasClass('active') && $frmCompanyOrgInfo.valid()) {
                        let addressDTO = undefined;
                        if (isNotEmpty($txtAddrBuildingNo.val()) || isNotEmpty($txtAddrRoadStreet.val()) || isNotEmpty($lstAddrRegion.val())
                            || isNotEmpty($lstAddrTownship.val()) || isNotEmpty($lstAddrWardVillage.val()) || isNotEmpty($txtAddrPostalCode.val())
                            || isNotEmpty($lstAddrCountry.val())) {
                            addressDTO = {
                                buildingNo: $txtAddrBuildingNo.val().trim(),
                                streetName: $txtAddrRoadStreet.val().trim(),
                                firstLevel: $lstAddrRegion.val(),
                                thirdLevel: $lstAddrTownship.val(),
                                fourthLevel: $lstAddrWardVillage.val(),
                                postalCode: $txtAddrPostalCode.val().trim(),
                                country: $lstAddrCountry.val()
                            };
                        }
                        let dto = {
                            customerType: 'COMPANY',
                            regNo: $txtRegNo.val().trim(),
                            name: $txtCompanyOrgName.val().trim(),
                            businessTypeId: $lstBusinessType.val(),
                            phone: $txtCompanyOrgPhone.val().trim(),
                            regCountry: $lstRegCountry.val(),
                            address: addressDTO,
                        };

                        createNewEntity(dto);
                    }
                }
            });

            // add click event to plugin's element
            $element.on("click", function () {
                events.onModalShow(options);
            });

            initValidation();
            // load related master datas
            initNRCStates(false);
            $lstNrcRegionCode.on("change", function (e) {
                loadNRCTownships($(this).find("option:selected").data("state-code"), $(this).closest('.nrc-container'));
            });
            initOccupations(false);
            initDivisions(false);

            initNationalities(false);
            $lstCitizenship.on("change", function (e) {
                let nationalityContainer = $(this).closest(".nationality-container");
                if (nationalityContainer.length === 1 && $(this).val() !== "Burmese") {
                    nationalityContainer.find('.race-selectpicker').closest('.bootstrap-select').removeClass('is-invalid');
                    nationalityContainer.find('.race-selectpicker').prop("disabled", true).selectpicker('refresh');
                }
                else {
                    nationalityContainer.find('.race-selectpicker').prop("disabled", false).selectpicker('refresh');
                }
            });

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

            if (plugin.settings.isIndividualEntityOnly !== true) {
                initBusinessTypes(false);
            }

            $modalEntityCreator.find(".address-container .state-division-selectpicker").on("change", function (e) {
                loadTownship($(this).find("option:selected").data("srcode"), $(this).closest('.address-container'));
            });

            $modalEntityCreator.find(".address-container .township-selectpicker").on("change", function (e) {
                loadWardVillage($(this).find("option:selected").data("tspcode"), $(this).closest('.address-container'));
            });

        }

        // private methods
        // these methods can be called only from inside the plugin like:
        // methodName(arg1, arg2, ... argn)
        let createNewEntity = function (dto) {
            let url = getApiResourcePath() + 'sec/entity/create';
            $.ajax({
                type: "POST",
                data: JSON.stringify(dto),
                url: url,
                success: function (response, textStatus, xhr) {
                    if (xhr.status === 200 && response.code === 200) {
                        if (response.createdData) {
                            events.onCreateEntity(response.createdData);
                        }
                        notify(response.title, response.message, "success");
                    }
                    else {
                        notify(response.title, response.message, "error");
                    }
                },
            });
        }

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
                setDefaultSelectToSelectPickers($lstAddrCountry, options, "");
                setDefaultSelectToSelectPickers($lstRegCountry, options, "Myanmar");
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
                            setDefaultSelectToSelectPickers($lstAddrCountry, options, "");
                            setDefaultSelectToSelectPickers($lstRegCountry, options, "Myanmar");
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

        let initNationalities = function (isByForce) {
            if (isByForce === false && plugin.settings.masterdatas.nationalities) {
                let options = [];
                options.push('<option value="" selected="selected">Not selected</option>');
                $.each(plugin.settings.masterdatas.nationalities, function (key, item) {
                    let option = '<option value="' + item.name + '">' + item.name + '</option>';
                    options.push(option);
                });
                setDefaultSelectToSelectPickers($lstCitizenship, options, "Burmese");
            }
            else {
                let criteria = {};
                $.ajax({
                    type: "GET",
                    async: false,
                    url: getApiResourcePath() + 'sec/master-data/general/nationalities',
                    data: criteria,
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            plugin.settings.masterdatas.nationalities = response;
                            let options = [];
                            options.push('<option value="" selected="selected">Not selected</option>');
                            $.each(response, function (key, item) {
                                let option = '<option value="' + item.name + '">' + item.name + '</option>';
                                options.push(option);
                            });
                            setDefaultSelectToSelectPickers($lstCitizenship, options, "Burmese");
                        }
                        else {
                            notify("Error", "Failed to fetch nationality informations.", "error");
                        }
                    }
                });
            }
        }

        let initOccupations = function (isByForce, selectedValue) {
            if (isByForce === false && plugin.settings.masterdatas.occupations) {
                let options = [];
                options.push('<option value="" selected="selected">Not selected</option>');
                $.each(plugin.settings.masterdatas.occupations, function (key, item) {
                    let option = '<option value="' + item.id + '">' + item.name + '</option>';
                    options.push(option);
                });
                setDefaultSelectToSelectPickers($lstOccupation, options, selectedValue);
            }
            else {
                let criteria = {};
                $.ajax({
                    type: "POST",
                    async: false,
                    url: getApiResourcePath() + 'sec/master-data/occupation/search/list',
                    data: JSON.stringify(criteria),
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            plugin.settings.masterdatas.occupations = response;
                            let options = [];
                            options.push('<option value="" selected="selected">Not selected</option>');
                            $.each(response, function (key, item) {
                                let option = '<option value="' + item.id + '">' + item.name + '</option>';
                                options.push(option);
                            });
                            // အသစ်ထည့်လိုက်တာဆိုရင် picker မှန်သမျှလိုက်ပြီး update လုပ်ဖို့လိုတယ်။
                            if (isByForce === true) {
                                $('.occupation-picker').selectpicker('destroy').html(options).selectpicker('refresh').selectpicker('render');
                                $lstOccupation.selectpicker('val', selectedValue).selectpicker('refresh');
                            }
                            else {
                                setDefaultSelectToSelectPickers($lstOccupation, options, selectedValue);
                            }
                        }
                        else {
                            notify("Error", "Failed to fetch occupation informations.", "error");
                        }
                    }
                });
            }
        }

        let initBusinessTypes = function (isByForce, selectedValue) {
            if (isByForce === false && plugin.settings.masterdatas.businessTypes) {
                let options = [];
                options.push('<option value="" selected="selected">Not selected</option>');
                $.each(plugin.settings.masterdatas.businessTypes, function (key, item) {
                    let option = '<option value="' + item.id + '">' + item.name + '</option>';
                    options.push(option);
                });
                setDefaultSelectToSelectPickers($lstBusinessType, options, selectedValue);
            }
            else {
                let criteria = {};
                $.ajax({
                    type: "POST",
                    async: false,
                    url: getApiResourcePath() + 'sec/master-data/business-type/search/list',
                    data: JSON.stringify(criteria),
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200 && response) {
                            plugin.settings.masterdatas.businessTypes = response;
                            let options = [];
                            options.push('<option value="" selected="selected">Not selected</option>');
                            $.each(response, function (key, item) {
                                let option = '<option value="' + item.id + '">' + item.name + '</option>';
                                options.push(option);
                            });

                            // အသစ်ထည့်လိုက်တာဆိုရင် picker မှန်သမျှလိုက်ပြီး update လုပ်ဖို့လိုတယ်။
                            if (isByForce === true) {
                                $('.business-type-picker').selectpicker('destroy').html(options).selectpicker('refresh').selectpicker('render');
                                $lstBusinessType.selectpicker('val', selectedValue).selectpicker('refresh');
                            }
                            else {
                                setDefaultSelectToSelectPickers($lstBusinessType, options, selectedValue);
                            }
                        }
                        else {
                            notify("Error", "Failed to fetch business type informations.", "error");
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

        let initValidation = function () {
            jQuery.validator.addMethod("validNrc", function (value, element, params) {
                if ($('.nav-link[href="#tab-entity-creator-individual' + $elementCount + '"]').hasClass('active')) {
                    if (isEmpty($txtPassportNo.val()) && $txtPassportNo.val().trim().length > 6) {
                        return !(isEmpty($lstNrcRegionCode.val()) || isEmpty($lstNrcTownshipCode.val()) || isEmpty($lstNrcType.val()) || isEmpty($txtNrcNo.val()));
                    }
                }
                return true;
            }, 'invalid/incomplete NRC value.');

            $frmIndividualInfo.validate({
                rules: {
                    'entityCreator.nrcNo': {
                        required: function (value, element) {
                            if ($('.nav-link[href="#tab-entity-creator-individual' + $elementCount + '"]').hasClass('active')) {
                                if (isEmpty($txtPassportNo.val())) {
                                    return true;
                                }
                            }
                            return false;
                        },
                        validNrc: function (value, element) {
                            return true;
                        },
                        minlength: 6,
                        maxlength: 6,
                    },
                    'entityCreator.passportNo': {
                        required: function (value, element) {
                            if ($('.nav-link[href="#tab-entity-creator-individual' + $elementCount + '"]').hasClass('active')) {
                                if (isEmpty($lstNrcRegionCode.val()) || isEmpty($lstNrcTownshipCode.val()) || isEmpty($lstNrcType.val()) || isEmpty($txtNrcNo.val())) {
                                    return true;
                                }
                            }
                            return false;
                        },
                        minlength: 6,
                        maxlength: 20
                    },
                    'entityCreator.individualName': {
                        required: function (value, element) {
                            return $('.nav-link[href="#tab-entity-creator-individual' + $elementCount + '"]').hasClass('active');
                        },
                        maxlength: 100
                    },
                    'entityCreator.individualFatherName': {
                        required: function (value, element) {
                            return $('.nav-link[href="#tab-entity-creator-individual' + $elementCount + '"]').hasClass('active');
                        },
                        maxlength: 100
                    },
                    'entityCreator.individualPhoneNo': {
                        required: function (value, element) {
                            return $('.nav-link[href="#tab-entity-creator-individual' + $elementCount + '"]').hasClass('active');
                        },
                        minlength: 7,
                        maxlength: 15
                    },
                    'entityCreator.individualDob': {
                        required: function (value, element) {
                            return $('.nav-link[href="#tab-entity-creator-individual' + $elementCount + '"]').hasClass('active');
                        },
                        minlength: 8,
                        maxlength: 10
                    },
                    'entityCreator.individualOccupation': {
                        required: function (value, element) {
                            return $('.nav-link[href="#tab-entity-creator-individual' + $elementCount + '"]').hasClass('active');
                        },
                    },
                },
                messages: {
                    'entityCreator.nrcNo': {
                        required: "`NRC Number` field must be present.",
                        minlength: "`NRC Number` should be minimum of (6) characters.",
                        maxlength: "`NRC Number` may not be greater than (6) characters.",
                    },
                    'entityCreator.passportNo': {
                        required: "`Passport Number` field must be present.",
                        minlength: "`Passport Number` should be minimum of (6) characters.",
                        maxlength: "`Passport Number` may not be greater than (20) characters.",
                    },
                    'entityCreator.individualName': {
                        required: "`Name` field must be present.",
                        maxlength: "`Name` may not be greater than (100) characters.",
                    },
                    'entityCreator.individualFatherName': {
                        required: "`Father Name` field must be present.",
                        maxlength: "`Father Name` may not be greater than (100) characters.",
                    },
                    'entityCreator.individualPhoneNo': {
                        required: "`Phone No` field must be present.",
                        minlength: "`Phone No` should be minimum of (7) characters.",
                        maxlength: "`Phone No` may not be greater than (15) characters.",
                    },
                    'entityCreator.individualDob': {
                        required: "`Date Of Birth` field must be present.",
                        minlength: "`Date Of Birth` should be minimum of (8) digits.",
                        maxlength: "`Date Of Birth` may not be greater than (10) digits.",
                    },
                    'entityCreator.individualOccupation': {
                        required: "`Occupation` field must be present.",
                    }
                }
            });

            $frmCompanyOrgInfo.validate({
                rules: {
                    'entityCreator.regNo': {
                        required: function (value, element) {
                            return $('.nav-link[href="#tab-entity-creator-company-org' + $elementCount + '"]').hasClass('active');
                        },
                        maxlength: 20
                    },
                    'entityCreator.companyOrgName': {
                        required: function (value, element) {
                            return $('.nav-link[href="#tab-entity-creator-company-org' + $elementCount + '"]').hasClass('active');
                        },
                        maxlength: 100
                    },
                },
                messages: {
                    'entityCreator.regNo': {
                        required: "`Registeration No.` field must be present.",
                        maxlength: "`Registeration No.` may not be greater than (20) characters.",
                    },
                    'entityCreator.companyOrgName': {
                        required: "`Name` field must be present.",
                        maxlength: "`Name` may not be greater than (100) characters.",
                    },
                }
            });
        }

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
    $.fn.EntityCreator = function (options) {

        // iterate through the DOM elements we are attaching the plugin to
        return this.each(function () {

            // if plugin has not already been attached to the element
            if (undefined === $(this).data('EntityCreator')) {

                // create a new instance of the plugin
                // pass the DOM element and the user-provided options as arguments
                let plugin = new $.EntityCreator(this, options);

                // in the jQuery version of the element
                // store a reference to the plugin object
                // you can later access the plugin and its methods and properties like
                // element.data('EntityCreator').publicMethod(arg1, arg2, ... argn) or
                // element.data('EntityCreator').settings.propertyName
                $(this).data('EntityCreator', plugin);
            }
        });
    }

})(jQuery);
