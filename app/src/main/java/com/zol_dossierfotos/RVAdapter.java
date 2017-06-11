// Package name.
package com.zol_dossierfotos;

// Imports from official sources.
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

// External source imports.
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;

// Adapter for our recyclerview.
class RVAdapter extends RecyclerView.Adapter<RVAdapter.photoViewHolder> {

     // Create a ViewHolder and get the view for our card object.
     class photoViewHolder extends RecyclerView.ViewHolder {

         LinearLayout cardLiner;
         ImageView photo;
         TextView categorie;

         photoViewHolder(View itemView) {
             super(itemView);

             // Get the UI elements.
             cardLiner = (LinearLayout)itemView.findViewById(R.id.cardLiner);
             photo = (ImageView)itemView.findViewById(R.id.photo);
             categorie = (TextView)itemView.findViewById(R.id.categorieText);

         }
    }

    private List<Photo> photos;
    private Context context;
    int selectedPosition;

    // Create our instance of the class.
    RVAdapter(List<Photo> photos,Context context){
        this.photos = photos;
        this.context = context;
    }

    // Attaches the object created to the recyclerview.
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    // Create our card object.
    @Override
    public photoViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup,
                false);
        return new photoViewHolder(v);
    }

    // Set our photo for the card object and set a listener.
    @Override
    public void onBindViewHolder(final photoViewHolder photoViewHolder, int position) {

        if(selectedPosition == photoViewHolder.getAdapterPosition()){
            // Highlight.
            photoViewHolder.cardLiner.setBackgroundColor(Color.RED);
        } else{
            // Leave as is.
            photoViewHolder.cardLiner.setBackgroundColor(Color.TRANSPARENT);
        }

        File file = new File(((photos.get(photoViewHolder.getAdapterPosition())).photoPath));
        // Set the photo.
        Glide.with(context).load(((photos.get(photoViewHolder.getAdapterPosition())).photoPath))
                .signature(new StringSignature(String.valueOf(file.lastModified())))
                .fitCenter().into(photoViewHolder.photo);
        photoViewHolder.categorie.setText(
                (photos.get(photoViewHolder.getAdapterPosition())).categorieDescription);

        // Set the ClickListener.
        photoViewHolder.photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Updating old as well as new positions.
                notifyItemChanged(selectedPosition);
                selectedPosition = photoViewHolder.getAdapterPosition();
                notifyItemChanged(selectedPosition);
                ((MainActivity) context).displaySelectedPhoto(
                        photoViewHolder.getAdapterPosition());
            }
        });

    }

    // Call to notifying the changed position for highlighting the new one.
    void drawBorder(int position) {
        notifyItemChanged(selectedPosition);
        selectedPosition = position;
        notifyItemChanged(selectedPosition);
    }

    // Get the number of card objects we currently have.
    @Override
    public int getItemCount() {
        return photos.size();
    }

}
