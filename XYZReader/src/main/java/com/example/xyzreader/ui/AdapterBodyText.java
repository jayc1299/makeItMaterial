package com.example.xyzreader.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;

import java.util.ArrayList;
import java.util.List;

public class AdapterBodyText extends RecyclerView.Adapter<AdapterBodyText.BodyHolder> {

	private List<String> bodyComponents;

	public AdapterBodyText(List<String> bodyComponents) {
		this.bodyComponents = bodyComponents;
	}

	@NonNull
	@Override
	public BodyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new BodyHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_body, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull BodyHolder holder, int position) {
		String bodyItem = bodyComponents.get(position);
		holder.titleView.setText(bodyItem);
	}

	@Override
	public int getItemCount() {
		return bodyComponents.size();
	}

	class BodyHolder extends RecyclerView.ViewHolder {
		TextView titleView;

		BodyHolder(View view) {
			super(view);
			titleView = (TextView) view.findViewById(R.id.item_body_text);
		}
	}

}