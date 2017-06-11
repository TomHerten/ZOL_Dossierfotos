// Package name.
package com.zol_dossierfotos;

/** Creates a photo object **/
class Photo {

        String photoPath;
        String categorieKey;
        String categorieDescription;
        String encodedB64;
        String angle;

        Photo(String photoPath) {

            this.photoPath = photoPath;

        }

    String getPhotoPath() {
        return photoPath;
    }

    String getCategorieDescription() {
        return categorieDescription;
    }

    String getEncodedB64() {
        return encodedB64;
    }

}
