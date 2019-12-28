package cz.vitskalicky.lepsirozvrh.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import cz.vitskalicky.lepsirozvrh.items.RozvrhHodinaCaption;

public class CaptionView extends CellView {
    private RozvrhHodinaCaption caption = null;
    private String startTime = "";
    private String endTime = "";
    private String captionText = "";

    public CaptionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setDrawDividers(false, true, true);
    }

    public RozvrhHodinaCaption getCaption() {
        return caption;
    }

    public void setCaption(RozvrhHodinaCaption caption) {
        this.caption = caption;
        if (caption == null){
            startTime = "";
            endTime = "";
            captionText = "";
        }else {
            startTime = caption.getBegintime();
            endTime = caption.getEndtime();
            captionText = caption.getCaption();
        }
        invalidate();
        requestLayout();
    }

    @Override
    public int getMinimumHeight() {
        int timeHeight = (int) Math.max(secondaryTextPaint.measureText(startTime), secondaryTextPaint.measureText(startTime));
        int captionHeight = primaryTextSize;
        return super.getMinimumHeight() + Math.max(captionHeight, timeHeight);
    }

    @Override
    public int getMinimumWidth() {
        return (int) (super.getSuggestedMinimumWidth() + secondaryTextSize + primaryTextPaint.measureText(captionText) + secondaryTextSize);
    }

    @Override
    protected void onDrawContent(Canvas canvas, int xStart, int yStart, int xEnd, int yEnd) {
        int w = xEnd - xStart;
        int h = yEnd - yStart;

        int actualPrimaryTextSize = primaryTextSize;
        int actualSecondaryTextSize = secondaryTextSize;

        if (actualPrimaryTextSize > h){
            actualPrimaryTextSize = h;
        }
        float startTimeLenght = secondaryTextPaint.measureText(startTime);
        float endTimeLenght = secondaryTextPaint.measureText(endTime);
        if (Math.max(startTimeLenght, endTimeLenght) > h){
            if (startTimeLenght > endTimeLenght){
                actualSecondaryTextSize = (int) (startTimeLenght / (startTimeLenght / h));
            }else{
                actualSecondaryTextSize = (int) (endTimeLenght / (endTimeLenght / h));
            }
        }
        primaryTextPaint.setTextSize(actualPrimaryTextSize);
        secondaryTextPaint.setTextSize(actualSecondaryTextSize);

        primaryTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(captionText, w/2f, (h + actualPrimaryTextSize)/2f, primaryTextPaint);

        canvas.save();

        canvas.rotate(-90);

        //TODO: finish this
        canvas.drawText(startTime, 0,30, secondaryTextPaint);

        canvas.restore();
    }
}