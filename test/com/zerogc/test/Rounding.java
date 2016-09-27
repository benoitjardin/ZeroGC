package com.zerogc.test;

public class Rounding {
	public static void main(String[] args) {
		double d = -1.999951;
		int sign = d < 0 ? -1 : 1;
		long pi = (long)(d + 0.00005);
		double r = (d-pi)*10000 + 0.5;
		long pd = (long)(r);

		System.out.println(Math.round(d * 10000));

		System.out.println(pi);
		System.out.println(sign * r);
		System.out.println(sign * pd);
	}
}
