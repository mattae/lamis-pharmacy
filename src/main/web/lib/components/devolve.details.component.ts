import {Component, OnInit} from '@angular/core';
import {CardViewDateItemModel, CardViewItem, CardViewTextItemModel, NotificationService} from '@alfresco/adf-core';
import {Devolve, RelatedCD4, RelatedClinic, RelatedPharmacy, RelatedViralLoad} from '../model/pharmacy.model';
import {ActivatedRoute, Router} from '@angular/router';
import {TdDialogService} from '@covalent/core';
import {DevolveService} from '../services/devolve.service';

@Component({
    selector: 'devolve-details',
    templateUrl: './devolve.details.component.html'
})
export class DevolveDetailsComponent implements OnInit {
    properties: CardViewItem[] = [];
    entity: Devolve;
    relatedClinic: RelatedClinic;
    relatedPharmacy: RelatedPharmacy;
    relatedCD4: RelatedCD4;
    relatedViralLoad: RelatedViralLoad;

    constructor(private router: Router, private route: ActivatedRoute, private devolveService: DevolveService,
                private _dialogService: TdDialogService,
                private notificationService: NotificationService) {
    }

    ngOnInit() {
        this.route.data.subscribe(({entity}) => {
            this.entity = !!entity && entity.body ? entity.body : entity;
            this.buildProperties();
        });
    }

    edit() {
        this.router.navigate(['/', 'devolves', this.entity.uuid, 'patient', this.entity.patient.uuid, 'edit']);
    }

    delete() {
        this._dialogService.openConfirm({
            title: 'Confirm',
            message: 'Do you want to delete this client devolve, action cannot be reversed?',
            cancelButton: 'No',
            acceptButton: 'Yes',
            width: '500px',
        }).afterClosed().subscribe((accept: boolean) => {
            if (accept) {
                this.devolveService.delete(this.entity.id).subscribe((res) => {
                    if (res.ok) {
                        this.router.navigate(['patients']);
                    } else {
                        this.notificationService.showError('Error deleting devolve, please try again');
                    }
                });
            } else {
                // DO SOMETHING ELSE
            }
        });
    }

    buildProperties() {
        this.properties.push(new CardViewDateItemModel({
            key: 'ds',
            value: this.entity.dateDevolved,
            label: 'Date of Devolvement',
            format: 'dd MMM, yyyy'
        }));
        let type = 'MMD';
        switch (this.entity.dmocType) {
            case 'ARC':
                type = 'Adolescent Refill Club';
                break;
            case 'CPARP':
                type = 'CPARP';
                break;
            case 'CARC':
                type = 'CARC';
                break;
            case 'F_CARG':
                type = 'F-CARG';
                break;
            case 'FAST_TRACK':
                type = 'Fast Track';
                break;
            case 'S_CARG':
                type = 's-CARG';
                break;
            case 'MMS':
                type = 'MMS';
                break;
        }
        this.properties.push(new CardViewTextItemModel({
            key: 'ds',
            value: type,
            label: 'Type of DMOC'
        }));
        this.devolveService.getRelatedClinic(this.entity.id, this.entity.patient.id, this.entity.dateDevolved).subscribe(res => {
            this.relatedClinic = res;
            console.log('Related clinic', res);
            if (this.relatedClinic.dateVisit) {
                this.properties.push(new CardViewTextItemModel({
                    key: 'ds',
                    value: this.relatedClinic.dateVisit,
                    label: 'Date of Clinical Stage'
                }));
                this.properties.push(new CardViewTextItemModel({
                    key: 'ds',
                    value: this.relatedClinic && this.relatedClinic.clinicStage,
                    label: 'Current Clinical Stage',
                }));
            }
        });
        this.devolveService.getRelatedPharmacy(this.entity.id, this.entity.patient.id, this.entity.dateDevolved).subscribe(res => {
            this.relatedPharmacy = res;
            if (this.relatedPharmacy.dateVisit) {
                this.properties.push(new CardViewTextItemModel({
                    key: 'ds',
                    value: this.relatedPharmacy.dateVisit,
                    label: 'Date of Current ARV Regimen'
                }));
                this.properties.push(new CardViewTextItemModel({
                    key: 'ds',
                    value: this.relatedPharmacy && this.relatedPharmacy.regimen,
                    label: 'Current ARV Regimen',
                }));
            }
        });
        this.devolveService.getRelatedViralLoad(this.entity.id, this.entity.patient.id, this.entity.dateDevolved).subscribe(res => {
            this.relatedViralLoad = res;
            if (this.relatedViralLoad.dateResultReceived) {
                this.properties.push(new CardViewTextItemModel({
                    key: 'ds',
                    value: this.relatedViralLoad.dateResultReceived,
                    label: 'Date of Viral Load'
                }));
                this.properties.push(new CardViewTextItemModel({
                    key: 'ds',
                    value: this.relatedViralLoad && this.relatedViralLoad.value,
                    label: 'Current Viral Load',
                }));
            }
        });
        this.devolveService.getRelatedCD4(this.entity.id, this.entity.patient.id, this.entity.dateDevolved).subscribe(res => {
            this.relatedCD4 = res;
            if (this.relatedCD4.dateResultReceived) {
                this.properties.push(new CardViewTextItemModel({
                    key: 'ds',
                    value: this.relatedCD4.dateResultReceived,
                    label: 'Date of CD4'
                }));
                this.properties.push(new CardViewTextItemModel({
                    key: 'ds',
                    value: this.relatedCD4 && this.relatedCD4.value,
                    label: 'Current Viral Load',
                }));
            }
        });
        this.properties.push(new CardViewDateItemModel({
            key: 'ds',
            value: this.entity.dateNextClinic,
            label: 'Date of next Clinic/Lab',
            format: 'dd MMM, yyyy'
        }));
        this.properties.push(new CardViewDateItemModel({
            key: 'ds',
            value: this.entity.dateNextRefill,
            label: 'Date of Viral Load',
            format: 'dd MMM, yyyy'
        }));
        if (this.entity.communityPharmacy) {
            this.properties.push(new CardViewTextItemModel({
                key: 'ds',
                value: this.entity.communityPharmacy.name,
                label: 'Community Pharmacy'
            }));
        }
        if (this.entity.dateDiscontinued) {
            this.properties.push(new CardViewDateItemModel({
                key: 'ds',
                value: this.entity.dateDiscontinued,
                label: 'Date of Discontinuation',
                format: 'dd MMM, yyyy'
            }));

            this.properties.push(new CardViewTextItemModel({
                key: 'ds',
                value: this.entity.reasonDiscontinued,
                label: 'Reason of Discontinuation'
            }));

            if (this.entity.dateReturnedToFacility) {
                this.properties.push(new CardViewDateItemModel({
                    key: 'ds',
                    value: this.entity.dateReturnedToFacility,
                    label: 'Date Returned to Facility',
                    format: 'dd MMM, yyyy'
                }));
            }
        }
    }

    previousState() {
        window.history.back();
    }
}
