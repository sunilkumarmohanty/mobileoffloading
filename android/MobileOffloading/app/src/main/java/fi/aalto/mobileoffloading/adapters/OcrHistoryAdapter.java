package fi.aalto.mobileoffloading.adapters;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import fi.aalto.mobileoffloading.OcrResultActivity;
import fi.aalto.mobileoffloading.OperatingMode;
import fi.aalto.mobileoffloading.R;
import fi.aalto.mobileoffloading.SourceMode;
import fi.aalto.mobileoffloading.models.OcrHistoryEntry;

public class OcrHistoryAdapter extends ArrayAdapter<OcrHistoryEntry> {

    private Activity context;
    private List<OcrHistoryEntry> ocrHistoryEntries;
    private Map<String, byte[]> thumbnails;

    static class OcrHistoryItemHolder {
        ImageView thumbnail;
        TextView text;
    }

    public OcrHistoryAdapter(Activity context, int layoutResourceId,
                             List<OcrHistoryEntry> ocrHistoryEntries, Map<String, byte[]> thumbnails) {
        super(context, layoutResourceId, ocrHistoryEntries);
        this.context = context;
        this.ocrHistoryEntries = ocrHistoryEntries;
        this.thumbnails = thumbnails;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final OcrHistoryItemHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            convertView = inflater.inflate(R.layout.history_item_view, parent, false);

            viewHolder = new OcrHistoryItemHolder();
            viewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.history_item_thumbnail);
            viewHolder.text = (TextView) convertView.findViewById(R.id.history_item_text);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (OcrHistoryItemHolder) convertView.getTag();
        }

        final OcrHistoryEntry ocrHistoryEntry = getItem(position);
        viewHolder.text.setText(ocrHistoryEntry.getText());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultActivity = new Intent(context, OcrResultActivity.class);
                resultActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                ArrayList<Uri> images = new ArrayList<>();
                for (String image : ocrHistoryEntry.getOriginals()) {
                    images.add(Uri.parse(context.getResources().getString(R.string.server_url)
                            + "/api/" + image));
                }

                resultActivity.putExtra("IMAGELIST", images);
                resultActivity.putExtra("OCRText", ocrHistoryEntry.getText());
                resultActivity.putExtra("SOURCEMODE", SourceMode.HISTORY.toString());
                resultActivity.putExtra("OPERATINGMODE", OperatingMode.NONE.toString());

                Date creationDate = new Date();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                try {
                    creationDate = df.parse(ocrHistoryEntry.getCreatedAt());
                } catch (ParseException e) {
                    Log.w("DATE", "Can't parse createdAt. Current date will be used.");
                    e.printStackTrace();
                }
                df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                resultActivity.putExtra("CREATIONTIME", df.format(creationDate));

                context.startActivity(resultActivity);
            }
        });

        byte[] picture = thumbnails.get(ocrHistoryEntry.getThumbnails().get(0));
        if (picture != null) {
            Glide.with(context)
                    .load(picture)
                    .crossFade()
                    .into(viewHolder.thumbnail);
        }

        return convertView;
    }
}
